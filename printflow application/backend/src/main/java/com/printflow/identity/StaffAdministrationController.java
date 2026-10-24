package com.printflow.identity;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StaffAdministrationController {
    public enum Role { OWNER,SALES,ESTIMATOR,DESIGNER,SUPERVISOR,OPERATOR,INVENTORY,ACCOUNTS,DELIVERY }
    public record Account(UUID id,String username,String role,boolean enabled,boolean mustChangePassword,boolean mfaEnrolled,boolean contactsAccess) {}
    public record Create(@NotBlank @Pattern(regexp="[a-zA-Z0-9@._+\\-]{3,140}") String username,@NotNull Role role,@NotBlank @Size(max=72) String temporaryPassword) {}
    public record Access(@NotNull Role role,@NotNull Boolean enabled) {}
    public record Permissions(@NotNull Boolean contactsAccess) {}
 public record Password(@NotBlank @Size(max=72) String password) {}
    public record ChangePassword(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(max=72) String newPassword) {}
    public record Activity(UUID id,UUID actorId,UUID targetId,String action,Instant createdAt) {}
    private final JdbcTemplate jdbc;private final PasswordEncoder passwords;
    public StaffAdministrationController(JdbcTemplate jdbc,PasswordEncoder passwords){this.jdbc=jdbc;this.passwords=passwords;}
    @GetMapping("/api/v1/owner/staff") public ResponseEntity<List<Account>> list(){
        return response(jdbc.query("select id,username,role,enabled,must_change_password,contacts_access,totp_secret is not null as enrolled from staff_users where role<>'CUSTOMER' order by enabled desc,created_at desc,id limit 500",(rs,n)->new Account(rs.getObject("id",UUID.class),rs.getString("username"),rs.getString("role"),rs.getBoolean("enabled"),rs.getBoolean("must_change_password"),rs.getBoolean("enrolled"),rs.getString("role").equals("OWNER")||rs.getBoolean("contacts_access"))));
    }
    @PostMapping("/api/v1/owner/staff") @Transactional
    public ResponseEntity<Map<String,UUID>> create(@AuthenticationPrincipal StaffPrincipal actor,HttpSession session,@Valid @RequestBody Create input){
        recent(session);policy(input.temporaryPassword());UUID id=UUID.randomUUID();
        int count=jdbc.update("insert into staff_users(id,username,password_hash,role,must_change_password) values(?,?,?,?,true) on conflict(username) do nothing",id,input.username().trim().toLowerCase(Locale.ROOT),passwords.encode(input.temporaryPassword()),input.role().name());
        if(count!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Username already exists");audit(actor.id(),id,"STAFF_CREATED");return response(Map.of("id",id));
    }
    @PatchMapping("/api/v1/owner/staff/{id}/access") @Transactional
    public ResponseEntity<Map<String,String>> access(@AuthenticationPrincipal StaffPrincipal actor,HttpSession session,@PathVariable UUID id,@Valid @RequestBody Access input){
        recent(session);lockAdministration();
        if(id.equals(actor.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Another owner must change your own access");
        String previous=jdbc.query("select role from staff_users where id=? and role<>'CUSTOMER' for update",(rs,n)->rs.getString(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(previous.equals("OWNER")&&(!input.enabled()||input.role()!=Role.OWNER)&&jdbc.queryForObject("select count(*) from staff_users where role='OWNER' and enabled",Integer.class)<=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Keep at least one enabled owner");
        // Versioning revokes old sessions even when a role is later restored.
        jdbc.update("update staff_users set role=?,enabled=?,auth_version=auth_version+1 where id=?",input.role().name(),input.enabled(),id);
        audit(actor.id(),id,"STAFF_ACCESS_CHANGED");return response(Map.of("code","ACCESS_CHANGED"));
    }
    @PatchMapping("/api/v1/owner/staff/{id}/permissions") @Transactional
    public ResponseEntity<Map<String,String>> permissions(@AuthenticationPrincipal StaffPrincipal actor,HttpSession session,@PathVariable UUID id,@Valid @RequestBody Permissions input){
        recent(session);if(id.equals(actor.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Owners already have Contacts access");
        int changed=jdbc.update("update staff_users set contacts_access=?,auth_version=auth_version+1 where id=? and role not in ('CUSTOMER','OWNER')",input.contactsAccess(),id);
        if(changed!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND);audit(actor.id(),id,input.contactsAccess()?"CONTACTS_GRANTED":"CONTACTS_REVOKED");return response(Map.of("code","PERMISSIONS_CHANGED"));
    }
    @PostMapping("/api/v1/owner/staff/{id}/password") @Transactional
    public ResponseEntity<Map<String,String>> reset(@AuthenticationPrincipal StaffPrincipal actor,HttpSession session,@PathVariable UUID id,@Valid @RequestBody Password input){
        recent(session);policy(input.password());if(id.equals(actor.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Use change password for your own account");
        if(jdbc.update("update staff_users set password_hash=?,must_change_password=true where id=? and role<>'CUSTOMER'",passwords.encode(input.password()),id)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        audit(actor.id(),id,"STAFF_PASSWORD_RESET");return response(Map.of("code","PASSWORD_RESET"));
    }
    @PostMapping("/api/v1/auth/password") @Transactional
    public ResponseEntity<Map<String,String>> change(@AuthenticationPrincipal StaffPrincipal actor,HttpSession session,@Valid @RequestBody ChangePassword input){
        policy(input.newPassword());jdbc.queryForObject("select id from staff_users where id=? for update",UUID.class,actor.id());
        if(actor.role().equals("CUSTOMER")&&actor.mustChangePassword()&&Boolean.TRUE.equals(jdbc.queryForObject("select setup_pending from customer_profiles where user_id=?",Boolean.class,actor.id())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete customer account activation and privacy consent first");
        String current=jdbc.queryForObject("select password_hash from staff_users where id=?",String.class,actor.id());
        if(!passwords.matches(input.currentPassword(),current))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect");
        if(passwords.matches(input.newPassword(),current))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a different password");
        jdbc.update("update staff_users set password_hash=?,must_change_password=false where id=?",passwords.encode(input.newPassword()),actor.id());audit(actor.id(),actor.id(),"PASSWORD_CHANGED");session.invalidate();return response(Map.of("code","SIGN_IN_AGAIN"));
    }
    @GetMapping("/api/v1/owner/activity") public ResponseEntity<List<Activity>> activities(){return response(jdbc.query("select id,actor_id,target_id,action,created_at from security_activities order by created_at desc,id limit 100",(rs,n)->new Activity(rs.getObject("id",UUID.class),rs.getObject("actor_id",UUID.class),rs.getObject("target_id",UUID.class),rs.getString("action"),rs.getTimestamp("created_at").toInstant())));}
    private void lockAdministration(){jdbc.execute("select pg_advisory_xact_lock(8675309)");}
    private void recent(HttpSession session){if(!OwnerMfaController.recent(session))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify owner MFA again before administration");}
    private void policy(String value){if(value.length()<14||value.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must have at least 14 characters and at most 72 UTF-8 bytes");}
    private void audit(UUID actor,UUID target,String action){jdbc.update("insert into security_activities(id,actor_id,target_id,action) values(?,?,?,?)",UUID.randomUUID(),actor,target,action);}
    private <T> ResponseEntity<T> response(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
