package com.printflow.identity;
import java.util.*;import java.nio.charset.StandardCharsets;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Service;
/** Called inside the domain transaction. Queuing never performs an SMTP request. */
@Service public class ProjectNotifications {
 private final JdbcTemplate db;private final CustomerMail mail;public ProjectNotifications(JdbcTemplate db,CustomerMail mail){this.db=db;this.mail=mail;}
 public void customer(UUID enquiry,String kind,UUID event,String text){customer(enquiry,kind,event,text,null);}
 private void customer(UUID enquiry,String kind,UUID event,String text,UUID feedback){var e=db.queryForMap("select customer_user_id,email,public_id from enquiries where id=?",enquiry);mail.queueProject((UUID)e.get("customer_user_id"),enquiry,e.get("email").toString(),e.get("public_id").toString(),kind,event,text,feedback);}
 public void team(UUID enquiry,String kind,UUID event,String text){team(enquiry,kind,event,text,null);}
 private void team(UUID enquiry,String kind,UUID event,String text,UUID feedback){var e=db.queryForMap("select public_id from enquiries where id=?",enquiry);Set<String> recipients=new LinkedHashSet<>();recipients.add(mail.ownerNotificationAddress());for(var r:db.queryForList("select username from staff_users where enabled and id in (select assigned_staff_id from enquiries where id=? union select designer_id from enquiries where id=? union select staff_id from staff_job_tasks where enquiry_id=? union select operator_id from production_jobs where enquiry_id=?)",enquiry,enquiry,enquiry,enquiry))recipients.add(r.get("username").toString());for(String address:recipients){if(address==null||!address.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||address.endsWith(".local"))continue;UUID audience=UUID.nameUUIDFromBytes((event+":"+address.toLowerCase(Locale.ROOT)+":team").getBytes(StandardCharsets.UTF_8));mail.queueProject(null,enquiry,address,e.get("public_id").toString(),kind,audience,text,feedback,true);}}
 public void both(UUID enquiry,String kind,UUID event,String text){customer(enquiry,kind,event,text);team(enquiry,kind,event,text);}
 public void referenceReview(UUID enquiry,UUID feedback,String text){customer(enquiry,"REFERENCE_REVIEW",feedback,text,feedback);team(enquiry,"REFERENCE_REVIEW",feedback,text,feedback);}
}
