package com.printflow.identity;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth/mfa")
public class OwnerMfaController {
    public static final String VERIFIED="OWNER_MFA_AT";
    private final JdbcTemplate jdbc;private final MfaSecrets secrets;
    public OwnerMfaController(JdbcTemplate jdbc,MfaSecrets secrets){this.jdbc=jdbc;this.secrets=secrets;}
    public record Code(@NotBlank @Size(max=64) String code) {}
    private record Pending(String secret,long expires) implements java.io.Serializable {}
    public static boolean verified(HttpSession session){return session!=null&&session.getAttribute(VERIFIED) instanceof Long time&&time>Instant.now().getEpochSecond()-1800;}
    public static boolean recent(HttpSession session){return session!=null&&session.getAttribute(VERIFIED) instanceof Long time&&time>Instant.now().getEpochSecond()-300;}
    private void owner(StaffPrincipal staff){if(!staff.role().equals("OWNER"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
    @PostMapping("/enroll") @Transactional
    public ResponseEntity<Map<String,String>> enroll(@AuthenticationPrincipal StaffPrincipal staff,HttpSession session){
        owner(staff);if(!secrets.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"MFA key is not configured");
        String existing=jdbc.queryForObject("select totp_secret from staff_users where id=? for update",String.class,staff.id());
        if(existing!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"MFA already enrolled");
        String seed=Totp.secret();session.setAttribute("MFA_PENDING",new Pending(seed,Instant.now().getEpochSecond()+300));
        String uri="otpauth://totp/J.D.Printex:"+java.net.URLEncoder.encode(staff.username(),StandardCharsets.UTF_8)+"?secret="+seed+"&issuer=J.D.Printex&algorithm=SHA1&digits=6&period=30";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("secret",seed,"uri",uri));
    }
    @PostMapping("/confirm") @Transactional(noRollbackFor=ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> confirm(@AuthenticationPrincipal StaffPrincipal staff,HttpSession session,@Valid @RequestBody Code input){
        owner(staff);lockAndLimit(staff.id());
        if(jdbc.queryForObject("select totp_secret is not null from staff_users where id=?",Boolean.class,staff.id()))throw new ResponseStatusException(HttpStatus.CONFLICT);
        Object value=session.getAttribute("MFA_PENDING");
        if(!(value instanceof Pending pending)||pending.expires()<Instant.now().getEpochSecond())throw new ResponseStatusException(HttpStatus.CONFLICT,"Enrollment expired; start again");
        long step=Totp.match(pending.secret(),input.code(),Instant.now().getEpochSecond());
        if(step<0)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid MFA code");
        jdbc.update("update staff_users set totp_secret=?,totp_counter=?,mfa_failures=0 where id=?",secrets.encrypt(pending.secret()),step,staff.id());
        List<String> recovery=new ArrayList<>();
        for(int i=0;i<8;i++){byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);String code=HexFormat.of().formatHex(bytes);recovery.add(code);jdbc.update("insert into owner_recovery_codes(staff_id,code_hash) values(?,?)",staff.id(),hash(code));}
        session.removeAttribute("MFA_PENDING");session.setAttribute(VERIFIED,Instant.now().getEpochSecond());audit(staff.id(),"OWNER_MFA_ENROLLED");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("recoveryCodes",recovery));
    }
    @PostMapping("/verify") @Transactional(noRollbackFor=ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> verify(@AuthenticationPrincipal StaffPrincipal staff,HttpSession session,@Valid @RequestBody Code input){
        owner(staff);lockAndLimit(staff.id());
        String seed=jdbc.queryForObject("select totp_secret from staff_users where id=?",String.class,staff.id());
        if(seed==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Enroll MFA first");
        boolean accepted=false;
        if(input.code().matches("[0-9]{6}")){
            long step=Totp.match(secrets.decrypt(seed),input.code(),Instant.now().getEpochSecond());
            if(step>=0)accepted=jdbc.update("update staff_users set totp_counter=? where id=? and totp_counter<?",step,staff.id(),step)==1;
        } else if(input.code().matches("[0-9a-f]{32}")) accepted=jdbc.update("delete from owner_recovery_codes where staff_id=? and code_hash=?",staff.id(),hash(input.code()))==1;
        if(!accepted)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid or already used MFA code");
        jdbc.update("update staff_users set mfa_failures=0 where id=?",staff.id());session.setAttribute(VERIFIED,Instant.now().getEpochSecond());audit(staff.id(),"OWNER_MFA_VERIFIED");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("code","MFA_VERIFIED"));
    }
    private void lockAndLimit(UUID id){
        jdbc.queryForObject("select id from staff_users where id=? for update",UUID.class,id);
        jdbc.update("update staff_users set mfa_failures=0,mfa_window=now() where id=? and mfa_window<now()-interval '5 minutes'",id);
        if(jdbc.queryForObject("select mfa_failures from staff_users where id=?",Integer.class,id)>=6)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Wait five minutes before retrying MFA");
        jdbc.update("update staff_users set mfa_failures=mfa_failures+1 where id=?",id);
    }
    private void audit(UUID id,String action){jdbc.update("insert into security_activities(id,actor_id,target_id,action) values(?,?,?,?)",UUID.randomUUID(),id,id,action);}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
