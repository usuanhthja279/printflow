package com.printflow.files;

import com.printflow.identity.ProjectNotifications;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Acceptance already holds the enquiry lock. A transaction advisory lock serializes load selection. */
@Service public class DesignerRouting {
 private final JdbcTemplate db;private final ProjectNotifications mail;
 public DesignerRouting(JdbcTemplate db,ProjectNotifications mail){this.db=db;this.mail=mail;}
 @Transactional public void allocate(UUID id){
  var rows=db.queryForList("select designer_id,designer_assignment_mode,status,email from enquiries where id=? for update",id);
  if(rows.isEmpty())return;var e=rows.getFirst();
  if(e.get("designer_id")!=null||!"AUTO".equals(e.get("designer_assignment_mode"))||!"IN_PROGRESS".equals(e.get("status")))return;
  if(!db.queryForObject("select exists(select 1 from quote_versions where enquiry_id=? and state='ACCEPTED')",Boolean.class,id))return;
  db.queryForList("select pg_advisory_xact_lock(24002401)");
  var designers=db.queryForList("select s.id,coalesce(nullif(s.public_name,''),'Assigned designer') as name from staff_users s where s.enabled and s.role='DESIGNER' and s.designer_available and s.routing_pool=? and not s.must_change_password order by (select count(*) from enquiries e where e.designer_id=s.id and e.status='IN_PROGRESS' and not exists(select 1 from fulfilment_work f where f.enquiry_id=e.id)),coalesce((select max(a.created_at) from designer_allocations a where a.designer_id=s.id),s.created_at),s.id limit 1",e.get("email").toString().toLowerCase(Locale.ROOT).endsWith(".test")?"TEST":"BUSINESS");
  if(designers.isEmpty())return;UUID designer=(UUID)designers.getFirst().get("id"),event=UUID.randomUUID();
  db.update("update enquiries set designer_id=?,revision=revision+1,updated_at=now() where id=?",designer,id);
  db.update("insert into designer_allocations(id,enquiry_id,designer_id) values(?,?,?)",event,id,designer);
  db.update("insert into design_events(id,enquiry_id,actor_id,action,object_id) values(?,?,null,'DESIGNER_AUTO_ASSIGNED',?)",event,id,designer);
  String text="Designer assigned automatically: "+designers.getFirst().get("name")+". Reference artwork and customer comments are ready in the design workspace.";
  db.update("insert into work_events(id,enquiry_id,actor_id,kind,message) values(?,?,null,'DESIGN_ASSIGN',?)",event,id,text);
  mail.both(id,"DESIGN_ASSIGN",event,text);
 }
 @Scheduled(fixedDelay=60000) @Transactional public void retryWaiting(){
  for(var row:db.queryForList("select id from enquiries where status='IN_PROGRESS' and designer_id is null and designer_assignment_mode='AUTO' and exists(select 1 from quote_versions q where q.enquiry_id=enquiries.id and q.state='ACCEPTED') order by created_at,id limit 25 for update skip locked"))allocate((UUID)row.get("id"));
 }
 public List<Map<String,Object>> designers(){return db.queryForList("select s.id,s.username,s.public_name,s.designer_available,s.routing_pool,s.must_change_password,(select count(*) from enquiries e where e.designer_id=s.id and e.status='IN_PROGRESS' and not exists(select 1 from fulfilment_work f where f.enquiry_id=e.id)) as active_jobs from staff_users s where s.enabled and s.role='DESIGNER' order by s.username");}
 public int pendingAssignments(){return db.queryForObject("select count(*) from enquiries e where e.status='IN_PROGRESS' and e.designer_id is null and exists(select 1 from quote_versions q where q.enquiry_id=e.id and q.state='ACCEPTED')",Integer.class);}
}
