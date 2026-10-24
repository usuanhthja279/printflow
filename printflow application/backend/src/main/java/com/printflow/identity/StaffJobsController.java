package com.printflow.identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class StaffJobsController {
 public record Assign(@NotNull UUID staffId,@NotBlank @Size(max=200) String title,@NotNull @Size(max=2000) String notes,@NotNull LocalDate dueDate){}
 public record Progress(@Min(1) int revision,@NotBlank @Pattern(regexp="TODO|IN_PROGRESS|DONE") String state){}
 private static final Set<String> WORKERS=Set.of("ESTIMATOR","SUPERVISOR","OPERATOR","INVENTORY","ACCOUNTS","DELIVERY");
 private final JdbcTemplate db;
 public StaffJobsController(JdbcTemplate db){this.db=db;}
 @GetMapping("/api/v1/auth/jobs") public ResponseEntity<?> own(@AuthenticationPrincipal StaffPrincipal u){if(!WORKERS.contains(u.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return response(db.queryForList("select j.id,j.enquiry_id,j.title,j.notes,to_char(j.due_date,'YYYY-MM-DD') as due_date,j.state,j.revision,e.product_code,e.quantity from staff_job_tasks j join enquiries e on e.id=j.enquiry_id where j.staff_id=? and j.job_role=? and e.status='IN_PROGRESS' order by j.state='DONE',j.due_date,j.id limit 100",u.id(),u.role()));}
 @GetMapping("/api/v1/owner/enquiries/{id}/jobs") public ResponseEntity<?> list(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id){owner(u);return response(db.queryForList("select j.id,j.staff_id,s.username,j.job_role,j.title,j.notes,to_char(j.due_date,'YYYY-MM-DD') as due_date,j.state from staff_job_tasks j join staff_users s on s.id=j.staff_id where j.enquiry_id=? order by j.created_at,j.id limit 100",id));}
 @PostMapping("/api/v1/owner/enquiries/{id}/jobs") @Transactional public ResponseEntity<?> assign(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Assign input){owner(u);var lead=db.queryForList("select status from enquiries where id=? for update",id);if(lead.isEmpty())throw new NoSuchElementException();if(!"IN_PROGRESS".equals(lead.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Assign work only after quotation acceptance");String role=db.queryForList("select role from staff_users where id=? and enabled",input.staffId()).stream().findFirst().map(r->r.get("role").toString()).orElseThrow(NoSuchElementException::new);if(!WORKERS.contains(role))throw new IllegalArgumentException("Choose a task-specific staff account");if(db.queryForObject("select count(*) from staff_job_tasks where enquiry_id=?",Integer.class,id)>=100)throw new ResponseStatusException(HttpStatus.CONFLICT,"Task allowance reached");UUID job=UUID.randomUUID();db.update("insert into staff_job_tasks(id,enquiry_id,staff_id,job_role,title,notes,due_date,assigned_by) values(?,?,?,?,?,?,?,?)",job,id,input.staffId(),role,input.title().trim(),input.notes().trim(),input.dueDate(),u.id());audit(job,u,"ASSIGNED");return response(Map.of("id",job));}
 @PatchMapping("/api/v1/auth/jobs/{id}") @Transactional public ResponseEntity<?> progress(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Progress input){if(!WORKERS.contains(u.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);var rows=db.queryForList("select j.enquiry_id from staff_job_tasks j where j.id=? and j.staff_id=? and j.job_role=?",id,u.id(),u.role());if(rows.isEmpty())throw new NoSuchElementException();var lead=db.queryForList("select status from enquiries where id=? for update",rows.getFirst().get("enquiry_id"));if(lead.isEmpty()||!"IN_PROGRESS".equals(lead.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Work is not active");if(db.update("update staff_job_tasks set state=?,revision=revision+1 where id=? and revision=?",input.state(),id,input.revision())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Task changed; refresh");audit(id,u,input.state());return response(Map.of("code","TASK_UPDATED"));}
 private void owner(StaffPrincipal u){if(!u.role().equals("OWNER"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private void audit(UUID job,StaffPrincipal u,String action){db.update("insert into staff_job_events(id,job_id,actor_id,action) values(?,?,?,?)",UUID.randomUUID(),job,u.id(),action);}
 private ResponseEntity<?> response(Object data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
}
