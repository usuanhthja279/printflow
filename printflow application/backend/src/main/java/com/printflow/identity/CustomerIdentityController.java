package com.printflow.identity;

import com.printflow.enquiry.TokenUtil;
import com.printflow.shared.RequestLimits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/identity")
public class CustomerIdentityController {
 public static final String PRIVACY="2026-10-23";
 public record Register(@NotBlank @Size(max=140) String name,@NotBlank @Email @Size(max=140) String email,
  @NotBlank @Pattern(regexp="[+0-9() .-]{7,32}") String phone,@NotBlank @Size(max=72) String password,@AssertTrue boolean consent,@NotBlank String privacyVersion) {}
 public record EmailInput(@NotBlank @Email @Size(max=140) String email) {}
 public record Token(@NotBlank @Size(max=100) String token) {}
 public record Reset(@NotBlank @Size(max=100) String token,@NotBlank @Size(max=72) String password) {}
 public record Setup(@NotBlank @Size(max=100) String token,@NotBlank @Size(max=72) String password,@AssertTrue boolean consent,@NotBlank String privacyVersion){}
 private final CustomerSetup setup;
 private final JdbcTemplate jdbc;private final PasswordEncoder passwords;private final CustomerMail mail;private final RequestLimits limits;private final boolean enabled;private final boolean production,privacyApproved;
 public CustomerIdentityController(CustomerSetup setup,JdbcTemplate jdbc,PasswordEncoder passwords,CustomerMail mail,RequestLimits limits,@Value("${APP_CUSTOMER_ACCOUNTS_ENABLED:false}") boolean enabled,org.springframework.core.env.Environment env,@Value("${APP_PRIVACY_APPROVED:false}") boolean privacyApproved){this.setup=setup;this.jdbc=jdbc;this.passwords=passwords;this.mail=mail;this.limits=limits;this.enabled=enabled;this.production=env.matchesProfiles("prod");this.privacyApproved=privacyApproved;}
 @GetMapping("/status") public Map<String,Boolean> status(){return Map.of("accountsEnabled",enabled&&mail.available()&&(!production||privacyApproved&&mail.deliveryEnabled()),"deliveryEnabled",mail.deliveryEnabled());}
 private void available(){if(!enabled||!mail.available()||production&&(!privacyApproved||!mail.deliveryEnabled()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Customer accounts are not configured yet");}
 private void limit(HttpServletRequest request,String email){limits.require("identity-ip",request.getRemoteAddr(),20);limits.require("identity-email",email.toLowerCase(Locale.ROOT),5);}
 @PostMapping("/setup") public ResponseEntity<Map<String,String>> setup(@Valid @RequestBody Setup input,HttpServletRequest request){available();limits.require("account-setup",request.getRemoteAddr(),20);setup.finish(input.token(),input.password(),input.consent(),input.privacyVersion());return ok("ACCOUNT_READY");}
 @PostMapping("/register") @Transactional
 public ResponseEntity<Map<String,String>> register(@Valid @RequestBody Register input,HttpServletRequest request){
  available();limit(request,input.email());policy(input.password());if(!PRIVACY.equals(input.privacyVersion()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Review the current privacy notice");
  String email=input.email().trim().toLowerCase(Locale.ROOT);UUID id=UUID.randomUUID();String hash=passwords.encode(input.password());
  int inserted=jdbc.update("insert into staff_users(id,username,password_hash,role,enabled) values(?,?,?,'CUSTOMER',false) on conflict(username) do nothing",id,email,hash);
  if(inserted==1)jdbc.update("insert into customer_profiles(user_id,name,phone,privacy_version) values(?,?,?,?)",id,input.name().trim(),input.phone(),PRIVACY);
  var pending=jdbc.queryForList("select u.id,p.verification_binding from staff_users u join customer_profiles p on p.user_id=u.id where u.username=? and u.role='CUSTOMER' and not p.email_verified for update of u",email);
  if(!pending.isEmpty()&&pending.getFirst().get("verification_binding")==null)issue((UUID)pending.getFirst().get("id"),email,"VERIFY",request.getSession(false),hash);
  return accepted();
 }
 @PostMapping("/resend") @Transactional
 public ResponseEntity<Map<String,String>> resend(@Valid @RequestBody EmailInput input,HttpServletRequest request){
  available();limit(request,input.email());jdbc.query("select u.id,u.username from staff_users u join customer_profiles p on p.user_id=u.id where u.username=? and not p.email_verified and not p.setup_pending",(rs,n)->{issue(rs.getObject("id",UUID.class),rs.getString("username"),"VERIFY",request.getSession(false));return true;},input.email().trim().toLowerCase(Locale.ROOT));var pending=jdbc.queryForList("select e.id,e.customer_user_id,e.public_id from enquiries e join customer_profiles p on p.user_id=e.customer_user_id join staff_users u on u.id=p.user_id where u.username=? and p.setup_pending and not p.email_verified and e.status<>'CLOSED' order by e.created_at desc limit 1",input.email().trim().toLowerCase(Locale.ROOT));if(!pending.isEmpty()){var e=pending.getFirst();String access=setup.prepare((UUID)e.get("id"));mail.queueProject((UUID)e.get("customer_user_id"),(UUID)e.get("id"),input.email().trim().toLowerCase(Locale.ROOT),e.get("public_id").toString(),"INVITE",UUID.randomUUID(),access);}return accepted();
 }
 @PostMapping("/verify") @Transactional
 public ResponseEntity<Map<String,String>> verify(@Valid @RequestBody Token input,HttpServletRequest request){
  available();limits.require("identity-token",request.getRemoteAddr(),30);UUID user=consume(input.token(),"VERIFY",request.getSession(false));
  String candidate=jdbc.queryForObject("select candidate_password_hash from identity_tokens where digest=?",String.class,TokenUtil.sha256(input.token()));
  jdbc.update("update customer_profiles set email_verified=true,setup_pending=false,verification_binding=null,temporary_password_expires_at=null,privacy_version=?,consent_at=coalesce(consent_at,now()) where user_id=?",PRIVACY,user);jdbc.update("update staff_users set password_hash=coalesce(?,password_hash),enabled=true,must_change_password=false,auth_version=auth_version+1 where id=? and role='CUSTOMER'",candidate,user);
  jdbc.update("update identity_tokens set used_at=coalesce(used_at,now()),candidate_password_hash=null where user_id=?",user);jdbc.update("update customer_setup_invites set used_at=now() where user_id=? and used_at is null",user);return ok("EMAIL_VERIFIED");
 }
 @PostMapping("/forgot") @Transactional
 public ResponseEntity<Map<String,String>> forgot(@Valid @RequestBody EmailInput input,HttpServletRequest request){
  available();limit(request,input.email());jdbc.query("select u.id,u.username from staff_users u join customer_profiles p on p.user_id=u.id where u.username=? and u.enabled and p.email_verified",(rs,n)->{issue(rs.getObject("id",UUID.class),rs.getString("username"),"RESET",request.getSession(false));return true;},input.email().trim().toLowerCase(Locale.ROOT));return accepted();
 }
 @PostMapping("/reset") @Transactional
 public ResponseEntity<Map<String,String>> reset(@Valid @RequestBody Reset input,HttpServletRequest request){
  available();limits.require("identity-token",request.getRemoteAddr(),30);policy(input.password());UUID user=consume(input.token(),"RESET",request.getSession(false));
  jdbc.update("update staff_users set password_hash=?,auth_version=auth_version+1 where id=? and role='CUSTOMER'",passwords.encode(input.password()),user);return ok("PASSWORD_RESET");
 }
 private UUID consume(String token,String purpose,jakarta.servlet.http.HttpSession session){
  var owners=jdbc.queryForList("select user_id from identity_tokens where digest=? and purpose=?",TokenUtil.sha256(token),purpose);if(owners.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link is invalid, expired or already used");jdbc.queryForList("select id from staff_users where id=? for update",owners.getFirst().get("user_id"));
  if(purpose.equals("VERIFY")){
   var bindings=jdbc.query("select p.verification_binding from customer_profiles p join identity_tokens t on t.user_id=p.user_id where t.digest=? and t.purpose='VERIFY'",(rs,n)->rs.getString(1),TokenUtil.sha256(token));
   if(!bindings.isEmpty()&&bindings.getFirst()!=null){Object proof=session==null?null:session.getAttribute("SSO_VERIFY_PROOF");if(!(proof instanceof String text)||!TokenUtil.matches(text,bindings.getFirst()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Open this verification link in the browser used for provider signup");}
  }
  var ids=jdbc.query("update identity_tokens set used_at=now() where digest=? and purpose=? and used_at is null and expires_at>now() returning user_id",(rs,n)->rs.getObject(1,UUID.class),TokenUtil.sha256(token),purpose);
  if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link is invalid, expired or already used");return ids.getFirst();
 }
 private void issue(UUID id,String email,String purpose,jakarta.servlet.http.HttpSession session){issue(id,email,purpose,session,null);}
 private void issue(UUID id,String email,String purpose,jakarta.servlet.http.HttpSession session,String candidate){
  jdbc.queryForList("select id from staff_users where id=? for update",id);
  if(candidate==null&&purpose.equals("VERIFY")){var previous=jdbc.queryForList("select candidate_password_hash from identity_tokens where user_id=? and purpose='VERIFY' order by expires_at desc limit 1",id);if(!previous.isEmpty())candidate=(String)previous.getFirst().get("candidate_password_hash");}
  String binding=jdbc.queryForObject("select verification_binding from customer_profiles where user_id=?",String.class,id);
  if(binding!=null){Object proof=session==null?null:session.getAttribute("SSO_VERIFY_PROOF");if(!(proof instanceof String text)||!TokenUtil.matches(text,binding))return;}

  jdbc.update("update identity_tokens set used_at=now() where user_id=? and purpose=? and used_at is null",id,purpose);
  jdbc.update("update mail_outbox set payload=null,state='SUPERSEDED' where user_id=? and kind=? and state='PENDING'",id,purpose);
  String token=TokenUtil.newToken();jdbc.update("insert into identity_tokens(digest,user_id,purpose,expires_at,candidate_password_hash) values(?,?,?,now()+interval '30 minutes',?)",TokenUtil.sha256(token),id,purpose,candidate);mail.queue(id,email,purpose,token);
 }
 private void policy(String value){if(value.length()<14||value.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use at least 14 characters and at most 72 UTF-8 bytes");}
 private ResponseEntity<Map<String,String>> accepted(){return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("code","CHECK_EMAIL_IF_ELIGIBLE"));}
 private ResponseEntity<Map<String,String>> ok(String code){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("code",code));}
}
