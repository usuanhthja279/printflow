package com.printflow.customer;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class PrivacyController {
 public enum Kind { EXPORT,ERASURE }
 public record Request(@NotNull Kind kind) {}
 public record Resolution(@NotBlank @Pattern(regexp="RESOLVED|REJECTED") String status,@NotBlank @Size(max=1000) String resolution) {}
 private final JdbcTemplate jdbc;
 public PrivacyController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping("/api/v1/customer/privacy") public ResponseEntity<List<Map<String,Object>>> own(@AuthenticationPrincipal StaffPrincipal user){return response(jdbc.queryForList("select id,kind,status,created_at,resolved_at,resolution from privacy_requests where user_id=? order by created_at desc limit 50",user.id()));}
 @PostMapping("/api/v1/customer/privacy") @Transactional public ResponseEntity<Map<String,String>> request(@AuthenticationPrincipal StaffPrincipal user,@Valid @RequestBody Request request){
  jdbc.queryForObject("select id from staff_users where id=? for update",UUID.class,user.id());
  if(jdbc.queryForObject("select count(*) from privacy_requests where user_id=? and kind=? and status='OPEN'",Integer.class,user.id(),request.kind().name())>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"A request of this kind is already open");
  jdbc.update("insert into privacy_requests(id,user_id,kind) values(?,?,?)",UUID.randomUUID(),user.id(),request.kind().name());return response(Map.of("code","REQUEST_RECORDED"));
 }
 @GetMapping("/api/v1/owner/privacy") public ResponseEntity<List<Map<String,Object>>> review(){return response(jdbc.queryForList("select p.*,u.username from privacy_requests p join staff_users u on u.id=p.user_id order by p.created_at desc limit 100"));}
 @PatchMapping("/api/v1/owner/privacy/{id}") @Transactional public ResponseEntity<Map<String,String>> resolve(@AuthenticationPrincipal StaffPrincipal owner,@PathVariable UUID id,@Valid @RequestBody Resolution resolution){
  var ids=jdbc.query("update privacy_requests set status=?,resolution=?,resolved_at=now() where id=? and status='OPEN' returning user_id",(rs,n)->rs.getObject(1,UUID.class),resolution.status(),resolution.resolution(),id);
  if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Request is absent or already reviewed");jdbc.update("insert into security_activities(id,actor_id,target_id,action) values(?,?,?,'PRIVACY_REQUEST_REVIEWED')",UUID.randomUUID(),owner.id(),ids.getFirst());return response(Map.of("code","REVIEW_RECORDED"));
 }
 private <T> ResponseEntity<T> response(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
}
