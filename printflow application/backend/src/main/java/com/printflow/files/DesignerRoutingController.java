package com.printflow.files;
import com.printflow.identity.StaffPrincipal;import jakarta.validation.Valid;import java.util.*;import org.springframework.http.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;
@RestController public class DesignerRoutingController {
 public record Availability(boolean available){}
 private final DesignerRouting routing;private final JdbcTemplate db;
 public DesignerRoutingController(DesignerRouting routing,JdbcTemplate db){this.routing=routing;this.db=db;}
 @GetMapping("/api/v1/owner/design-routing") public ResponseEntity<?> list(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(routing.designers());}
 @GetMapping("/api/v1/owner/design-routing/summary") public ResponseEntity<?> summary(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("pendingAssignmentCount",routing.pendingAssignments()));}
 @PatchMapping("/api/v1/owner/design-routing/{id}") @Transactional public ResponseEntity<?> update(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Availability input){
  if(db.update("update staff_users set designer_available=? where id=? and enabled and role='DESIGNER'",input.available(),id)!=1)throw new NoSuchElementException();
  db.update("insert into security_activities(id,actor_id,target_id,action) values(?,?,?,'DESIGNER_AVAILABILITY')",UUID.randomUUID(),u.id(),id);
  return list();
 }
}
