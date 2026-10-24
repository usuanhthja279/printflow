package com.printflow.customer;
import com.printflow.identity.*;import jakarta.validation.constraints.*;import java.time.LocalDate;import java.util.*;import org.springframework.http.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;

@Service public class ProductionQueue {
 public record Change(@Min(1) int revision,@NotBlank @Pattern(regexp="ASSIGN|PRINTING|FINISHING|QC|REWORK|READY|HOLD|RESUME") String action,@NotBlank @Size(min=10,max=500) String note,@AssertTrue boolean confirmed,UUID operatorId,@Size(max=80) String machine,@Pattern(regexp="NORMAL|URGENT") String priority,LocalDate dueDate){}
 private final JdbcTemplate db;private final ProjectNotifications mail;
 public ProductionQueue(JdbcTemplate db,ProjectNotifications mail){this.db=db;this.mail=mail;}
 private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 public Map<String,Object> board(StaffPrincipal u){
  if(!Set.of("OWNER","OPERATOR").contains(u.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  var rows=db.queryForList("select e.id,e.public_id,e.product_code,e.quantity,coalesce(j.stage,'WAITING_RELEASE') as stage,coalesce(f.state,'NOT_STARTED') as fulfilment,j.operator_id,coalesce(nullif(s.public_name,''),s.username) as operator_name,j.machine,j.priority,j.hold_reason,j.revision,to_char(coalesce(j.due_date,q.proposed_date),'YYYY-MM-DD') as due_date from enquiries e join quote_versions q on q.enquiry_id=e.id and q.state='ACCEPTED' left join fulfilment_work f on f.enquiry_id=e.id left join production_jobs j on j.enquiry_id=e.id left join staff_users s on s.id=j.operator_id where e.status='IN_PROGRESS' and (? or j.operator_id=?) order by j.priority='URGENT' desc nulls last,coalesce(j.due_date,q.proposed_date),e.created_at,e.id limit 200",u.role().equals("OWNER"),u.id());
  return Map.of("owner",u.role().equals("OWNER"),"jobs",rows,"operators",u.role().equals("OWNER")?db.queryForList("select id,coalesce(nullif(public_name,''),username) as name from staff_users where role='OPERATOR' and enabled order by username"):List.of());
 }
 public Map<String,Object> view(StaffPrincipal u,UUID id){scope(u,id,false);var rows=db.queryForList("select j.*,to_char(j.due_date,'YYYY-MM-DD') as due_date,coalesce(nullif(s.public_name,''),s.username) as operator_name from production_jobs j left join staff_users s on s.id=j.operator_id where j.enquiry_id=?",id);var data=new LinkedHashMap<String,Object>();data.put("job",rows.isEmpty()?null:rows.getFirst());data.put("owner",u.role().equals("OWNER"));data.put("operators",u.role().equals("OWNER")?db.queryForList("select id,coalesce(nullif(public_name,''),username) as name from staff_users where enabled and role='OPERATOR' order by username"):List.of());return data;}
 private void scope(StaffPrincipal u,UUID id,boolean lock){if(!Set.of("OWNER","OPERATOR").contains(u.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);var rows=db.queryForList("select status from enquiries where id=? and (? or exists(select 1 from production_jobs where enquiry_id=? and operator_id=?))"+(lock?" for update":""),id,u.role().equals("OWNER"),id,u.id());if(rows.isEmpty())throw new NoSuchElementException();if(lock&&!"IN_PROGRESS".equals(rows.getFirst().get("status")))throw conflict("This work is no longer active");}
 public void released(UUID id){db.update("insert into production_jobs(enquiry_id,due_date) select ?,q.proposed_date from fulfilment_work f join quote_versions q on q.id=f.quote_id where f.enquiry_id=? on conflict do nothing",id,id);}
 public void qualityRequired(UUID id){var rows=db.queryForList("select stage,hold_reason from production_jobs where enquiry_id=?",id);if(rows.isEmpty()||!"QC".equals(rows.getFirst().get("stage"))||rows.getFirst().get("hold_reason")!=null)throw conflict("Complete printing, finishing and the quality-check queue before marking this job ready");}
 public void ready(UUID id){db.update("update production_jobs set stage='READY',revision=revision+1,updated_at=now() where enquiry_id=?",id);}
 @Transactional public void change(StaffPrincipal u,UUID id,Change input){
  scope(u,id,true);var rows=db.queryForList("select j.*,f.state as work_state from production_jobs j join fulfilment_work f on f.enquiry_id=j.enquiry_id where j.enquiry_id=?",id);if(rows.isEmpty())throw conflict("Owner must release production first");var job=rows.getFirst();
  if(((Number)job.get("revision")).intValue()!=input.revision())throw conflict("Queue changed; refresh first");if(!"PRODUCTION".equals(job.get("work_state")))throw conflict("Production is already complete");if(input.note().trim().length()<10)throw new IllegalArgumentException("Describe the actual operation or hold");String action=input.action(),stage=job.get("stage").toString();
  if(action.equals("ASSIGN")){
   if(!u.role().equals("OWNER"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   if(input.operatorId()!=null&&!db.queryForObject("select exists(select 1 from staff_users where id=? and role='OPERATOR' and enabled)",Boolean.class,input.operatorId()))throw new IllegalArgumentException("Choose an enabled printer");
   db.update("update production_jobs set operator_id=?,machine=?,priority=?,due_date=?,revision=revision+1,updated_at=now() where enquiry_id=?",input.operatorId(),input.machine()==null?"":input.machine().trim(),input.priority()==null?"NORMAL":input.priority(),input.dueDate(),id);
  }else if(action.equals("HOLD")){db.update("update production_jobs set hold_reason=?,revision=revision+1,updated_at=now() where enquiry_id=?",input.note().trim(),id);
  }else if(action.equals("RESUME")){if(job.get("hold_reason")==null)throw conflict("No production hold is open");db.update("update production_jobs set hold_reason=null,revision=revision+1,updated_at=now() where enquiry_id=?",id);
  }else{
   if(job.get("hold_reason")!=null)throw conflict("Resolve the production hold before advancing");
   String expected=switch(action){case "PRINTING"->"QUEUED";case "FINISHING"->"PRINTING";case "QC"->"FINISHING";case "REWORK","READY"->"QC";default->"INVALID";};if(!expected.equals(stage))throw conflict("Complete the current operation first");
   if(!action.equals("READY"))db.update("update production_jobs set stage=?,revision=revision+1,updated_at=now() where enquiry_id=?",action.equals("REWORK")?"PRINTING":action,id);
  }
  UUID event=UUID.randomUUID();String text="Production "+action.toLowerCase(Locale.ROOT).replace('_',' ')+": "+input.note().trim();db.update("insert into work_events(id,enquiry_id,actor_id,kind,message) values(?,?,?,'PRODUCTION_QUEUE',?)",event,id,u.id(),text);if(!action.equals("ASSIGN")&&!action.equals("READY"))mail.both(id,"PRODUCTION_UPDATE",event,text);else if(action.equals("ASSIGN"))mail.team(id,"PRODUCTION_UPDATE",event,text);
 }
}
