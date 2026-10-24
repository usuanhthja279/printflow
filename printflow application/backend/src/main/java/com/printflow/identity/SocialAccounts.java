package com.printflow.identity;
import com.printflow.enquiry.TokenUtil;import com.printflow.shared.RequestLimits;
import jakarta.servlet.http.*;import jakarta.validation.constraints.*;
import java.time.Instant;import java.util.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.core.env.Environment;import org.springframework.http.HttpStatus;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.oauth2.core.oidc.user.OidcUser;import org.springframework.security.web.context.HttpSessionSecurityContextRepository;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
@Service
public class SocialAccounts {
 public record Pending(String provider,String issuer,String subject,String email,String name,boolean trustedEmail,long expires) implements java.io.Serializable {}
 public record Link(UUID id,String provider,long version,String passwordDigest,long expires) implements java.io.Serializable {}
 public record Signup(@NotBlank @Email @Size(max=140) String email,@NotBlank @Size(max=140) String name,@NotBlank @Pattern(regexp="[+0-9() .-]{7,32}") String phone,@AssertTrue boolean consent,@NotBlank String privacyVersion) {}
 public record Result(StaffPrincipal user,boolean verificationRequired) {}
 private final JdbcTemplate jdbc;private final StaffDirectory directory;private final PasswordEncoder passwords;private final CustomerMail mail;private final RequestLimits limits;private final boolean registrationEnabled;
 public SocialAccounts(JdbcTemplate jdbc,StaffDirectory directory,PasswordEncoder passwords,CustomerMail mail,RequestLimits limits,Environment env,@Value("${APP_CUSTOMER_ACCOUNTS_ENABLED:false}") boolean enabled,@Value("${APP_PRIVACY_APPROVED:false}") boolean approved){this.jdbc=jdbc;this.directory=directory;this.passwords=passwords;this.mail=mail;this.limits=limits;this.registrationEnabled=enabled&&(!env.matchesProfiles("prod")||approved&&mail.deliveryEnabled());}
 public Pending pending(HttpSession session){Object value=session.getAttribute("SSO_PENDING");if(!(value instanceof Pending p)||p.expires()<Instant.now().getEpochSecond())throw new ResponseStatusException(HttpStatus.GONE,"Provider sign-up expired. Start again");return p;}
 @Transactional public String callback(String provider,OidcUser identity,HttpServletRequest request,HttpServletResponse response){
  String issuer=identity.getIssuer().toString(),subject=identity.getSubject();if(issuer.length()>300||subject.length()>255)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var session=request.getSession();Object linkValue=session.getAttribute("SSO_LINK");session.removeAttribute("SSO_LINK");
  var linked=jdbc.query("select user_id from external_identities where provider=? and issuer=? and subject=?",(rs,n)->rs.getObject(1,UUID.class),provider,issuer,subject);
  if(linkValue instanceof Link link){
   if(link.expires()<Instant.now().getEpochSecond()||!link.provider().equals(provider))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   StaffPrincipal user=load(link.id());if(!user.isEnabled()||user.authVersion()!=link.version()||!TokenUtil.sha256(user.password()).equals(link.passwordDigest()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   if(!linked.isEmpty()&&!linked.getFirst().equals(user.id()))throw new ResponseStatusException(HttpStatus.CONFLICT);
   if(linked.isEmpty())insert(provider,issuer,subject,user.id());audit(user.id(),"PROVIDER_LINKED");signIn(user,request,response);return "/customer";
  }
  if(!linked.isEmpty()){StaffPrincipal user=load(linked.getFirst());if(!user.enabled()){session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);SecurityContextHolder.clearContext();return "/customer?provider_error=verify_or_contact";}signIn(user,request,response);return "/customer";}
  String email=identity.getEmail()==null?"":identity.getEmail(),name=identity.getFullName()==null?"":identity.getFullName();
  boolean trusted=provider.equals("google")&&Boolean.TRUE.equals(identity.getEmailVerified());
  session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);SecurityContextHolder.clearContext();session.setAttribute("SSO_PENDING",new Pending(provider,issuer,subject,email.length()<=140?email:"",name.length()<=140?name:"",trusted,Instant.now().getEpochSecond()+600));request.changeSessionId();return "/customer?mode=social";
 }
 @Transactional public Result complete(Signup input,HttpSession session,String source){
  if(!registrationEnabled)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Customer sign-up is not configured");Pending p=pending(session);limits.require("social-signup",source,10);
  if(!input.consent()||!CustomerIdentityController.PRIVACY.equals(input.privacyVersion()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Review the current privacy notice");
  String email=input.email().trim().toLowerCase(Locale.ROOT);boolean trusted=p.trustedEmail()&&email.equalsIgnoreCase(p.email());
  if(!trusted&&!mail.available())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email verification must be configured");
  UUID id=UUID.randomUUID();int created=jdbc.update("insert into staff_users(id,username,password_hash,role,enabled) values(?,?,?,'CUSTOMER',?) on conflict(username) do nothing",id,email,passwords.encode(TokenUtil.newToken()),trusted);
  if(created!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sign in to your existing account, then connect this provider from your profile");
  String binding=null;if(!trusted){String proof=TokenUtil.newToken();session.setAttribute("SSO_VERIFY_PROOF",proof);binding=TokenUtil.sha256(proof);}
  jdbc.update("insert into customer_profiles(user_id,name,phone,email_verified,privacy_version,verification_binding) values(?,?,?,?,?,?)",id,input.name(),input.phone(),trusted,CustomerIdentityController.PRIVACY,binding);insert(p.provider(),p.issuer(),p.subject(),id);audit(id,"PROVIDER_SIGNUP");
  if(!trusted){String token=TokenUtil.newToken();jdbc.update("insert into identity_tokens(digest,user_id,purpose,expires_at) values(?,?,'VERIFY',now()+interval '30 minutes')",TokenUtil.sha256(token),id);mail.queue(id,email,"VERIFY",token);}
  session.removeAttribute("SSO_PENDING");return new Result(load(id),!trusted);
 }
 public void prepareLink(StaffPrincipal user,String provider,String password,HttpSession session){limits.require("provider-link",user.id().toString(),8);if(!user.role().equals("CUSTOMER")||password==null||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||!passwords.matches(password,user.password()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Confirm your current password before connecting");session.setAttribute("SSO_LINK",new Link(user.id(),provider,user.authVersion(),TokenUtil.sha256(user.password()),Instant.now().getEpochSecond()+300));}
 public void signIn(StaffPrincipal user,HttpServletRequest request,HttpServletResponse response){if(!user.role().equals("CUSTOMER")||!user.enabled())throw new ResponseStatusException(HttpStatus.FORBIDDEN);request.getSession().removeAttribute("SSO_PENDING");request.changeSessionId();var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()));SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,request,response);}
 private StaffPrincipal load(UUID id){String username=jdbc.queryForObject("select username from staff_users where id=? and role='CUSTOMER'",String.class,id);if(username==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN);return directory.loadUserByUsername(username);}
 private void insert(String provider,String issuer,String subject,UUID user){if(jdbc.queryForObject("select count(*) from external_identities where user_id=? and provider=?",Integer.class,user,provider)>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Provider already connected");jdbc.update("insert into external_identities(provider,issuer,subject,user_id) values(?,?,?,?)",provider,issuer,subject,user);}
 private void audit(UUID user,String action){jdbc.update("insert into security_activities(id,actor_id,target_id,action) values(?,?,?,?)",UUID.randomUUID(),user,user,action);}
}
