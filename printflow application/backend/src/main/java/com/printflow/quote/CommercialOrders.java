package com.printflow.quote;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CommercialOrders {
 public record Convert(@NotNull UUID quoteId){}
 public record Link(@NotNull UUID orderLineId,@NotNull UUID artifactId){}
 private final JdbcTemplate db;private final JsonMapper json;
 public CommercialOrders(JdbcTemplate db,JsonMapper json){this.db=db;this.json=json;}
 private void scope(StaffPrincipal u,UUID lead,boolean write){var rows=db.queryForList("select assigned_staff_id,customer_user_id,status from enquiries where id=?"+(write?" for update":""),lead);if(rows.isEmpty())throw new NoSuchElementException();var e=rows.getFirst();boolean access=u.role().equals("OWNER")||u.role().equals("SALES")&&(!"NEW".equals(e.get("status"))&&u.id().equals(e.get("assigned_staff_id")))||!write&&u.role().equals("CUSTOMER")&&u.id().equals(e.get("customer_user_id"));if(!access)throw new NoSuchElementException();if(write&&"CLOSED".equals(e.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Closed enquiries are read-only");}
 @Transactional(readOnly=true) public List<Map<String,Object>> list(StaffPrincipal u,UUID lead){scope(u,lead,false);var rows=db.queryForList("select o.id,o.quote_id,o.order_number,o.scope,o.total_paise,o.terms,to_char(o.agreed_date,'YYYY-MM-DD') as agreed_date,o.specification_snapshot::text as specification_snapshot,o.created_at,q.state='ACCEPTED' as current_agreement from commercial_orders o join quote_versions q on q.id=o.quote_id where o.enquiry_id=? order by o.order_number desc",lead);for(var row:rows){row.put("lines",db.queryForList("select id,position,description,quantity,unit_paise,tax_bps,total_paise,specifications::text as specifications from commercial_order_lines where order_id=? order by position",row.get("id")));row.put("artwork",db.queryForList("select l.order_line_id,l.artifact_id,a.display_name from commercial_order_artwork l join commercial_order_lines i on i.id=l.order_line_id join design_artifacts a on a.id=l.artifact_id where i.order_id=? and (?=false or a.kind='REFERENCE' or exists(select 1 from proof_revisions p where p.artifact_id=a.id))",row.get("id"),u.role().equals("CUSTOMER")));row.put("productionReleased",false);}return rows;}
 @Transactional public Map<String,Object> convert(StaffPrincipal u,UUID lead,Convert input){scope(u,lead,true);var quotes=db.queryForList("select * from quote_versions where id=? and enquiry_id=? and state='ACCEPTED'",input.quoteId(),lead);if(quotes.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Choose the current accepted quotation");var existing=db.queryForList("select id from commercial_orders where quote_id=?",input.quoteId());if(!existing.isEmpty())return Map.of("id",existing.getFirst().get("id"),"created",false);
  var q=quotes.getFirst();UUID id=UUID.randomUUID();db.update("insert into commercial_orders(id,enquiry_id,quote_id,scope,total_paise,terms,agreed_date,specification_snapshot,created_by) values(?,?,?,?,?,?,?,?::jsonb,?)",id,lead,input.quoteId(),q.get("scope"),q.get("total_paise"),q.get("terms"),q.get("proposed_date"),q.get("specification_snapshot").toString(),u.id());
  @SuppressWarnings("unchecked") List<Map<String,Object>> items=json.readValue(q.get("line_items").toString(),List.class);
  if(items.isEmpty())items=List.of(Map.of("description",q.get("scope").toString().substring(0,Math.min(q.get("scope").toString().length(),500)),"quantity",1,"unitPaise",q.get("total_paise"),"taxBps",0,"totalPaise",q.get("total_paise"),"specifications",Map.of("pricingBasis","ALL_INCLUSIVE_LOT")));
  int n=0;for(var item:items)db.update("insert into commercial_order_lines(id,order_id,position,description,quantity,unit_paise,tax_bps,total_paise,specifications) values(?,?,?,?,?,?,?,?,?::jsonb)",UUID.randomUUID(),id,++n,item.get("description"),item.get("quantity"),item.get("unitPaise"),item.get("taxBps"),item.get("totalPaise"),json.writeValueAsString(item.get("specifications")));
  db.update("insert into work_events(id,enquiry_id,actor_id,kind,message) values(?,?,?,'ORDER','Accepted quotation converted to commercial order; production remains unreleased')",UUID.randomUUID(),lead,u.id());return Map.of("id",id,"created",true);
 }
 @Transactional public Map<String,Object> link(StaffPrincipal u,UUID lead,Link input){scope(u,lead,true);boolean valid=db.queryForObject("select exists(select 1 from commercial_order_lines i join commercial_orders o on o.id=i.order_id join quote_versions q on q.id=o.quote_id join design_artifacts a on a.id=? where i.id=? and o.enquiry_id=? and a.enquiry_id=o.enquiry_id and a.state='CLEAN' and q.state='ACCEPTED')",Boolean.class,input.artifactId(),input.orderLineId(),lead);if(!valid)throw new ResponseStatusException(HttpStatus.CONFLICT,"Choose a clean file and a line of the current order");db.update("insert into commercial_order_artwork(order_line_id,artifact_id,linked_by) values(?,?,?) on conflict do nothing",input.orderLineId(),input.artifactId(),u.id());return Map.of("code","LINKED");}
}
