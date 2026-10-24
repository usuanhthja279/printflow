package com.printflow.catalog;
import com.printflow.enquiry.EnquiryDtos.CreateEnquiry;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ProductTemplates {
 public record Template(String code,String name,String description,long revision,boolean active,int minQuantity,int maxQuantity,List<Integer> gsmOptions,List<String> sizes,List<String> finishes) {}
 public record Edit(@NotNull Long revision,@NotBlank @Size(max=140) String name,@NotBlank @Size(max=1000) String description,boolean active,@Min(1) int minQuantity,@Max(10000000) int maxQuantity,@NotNull @Size(max=30) List<@Min(40) @Max(600) Integer> gsmOptions,@NotNull @Size(max=30) List<@NotBlank @Size(max=60) String> sizes,@NotNull @Size(min=1,max=30) List<@NotBlank @Size(max=80) String> finishes) {}
 private final JdbcTemplate jdbc;private final JsonMapper json;
 public ProductTemplates(JdbcTemplate jdbc,JsonMapper json){this.jdbc=jdbc;this.json=json;}
 public List<Template> list(boolean all){return jdbc.query("select * from product_templates"+(all?"":" where active")+" order by code",(rs,n)->read(rs));}
 public Template get(String code){return jdbc.query("select * from product_templates where code=?",(rs,n)->read(rs),code).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 public Template capture(CreateEnquiry input){
  // Retained historical product codes may still be used by staff and existing clients.
  var matches=jdbc.query("select * from product_templates where code=?",(rs,n)->read(rs),input.productCode());if(matches.isEmpty())return null;
  Template t=matches.getFirst();if(!t.active()||input.quantity()<t.minQuantity()||input.quantity()>t.maxQuantity())throw new IllegalArgumentException("Product is unavailable or quantity is outside its configured range");
  if(input.paperGsm()!=null&&!t.gsmOptions().isEmpty()&&!t.gsmOptions().contains(input.paperGsm()))throw new IllegalArgumentException("Choose an available paper weight or ask for advice");
  if(input.size()!=null&&!input.size().isBlank()&&!t.sizes().isEmpty()&&!t.sizes().contains(input.size()))throw new IllegalArgumentException("Choose an available size or ask for advice");
  if(input.finish()!=null&&!input.finish().isBlank()&&!t.finishes().contains(input.finish()))throw new IllegalArgumentException("Choose an available finish");
  if(input.printSides()!=null&&!Set.of("SINGLE","DOUBLE","UNSURE").contains(input.printSides()))throw new IllegalArgumentException("Invalid print sides");return t;
 }
 public String snapshot(Template t){return json.writeValueAsString(t);}
 @Transactional public Template edit(StaffPrincipal actor,String code,Edit e){
  jdbc.queryForList("select code from product_templates where code=? for update",code);Template old=get(code);
  if(old.revision()!=e.revision())throw new ResponseStatusException(HttpStatus.CONFLICT,"Template changed; reload first");
  if(e.minQuantity()>e.maxQuantity()||e.maxQuantity()<1||!e.finishes().contains("UNSURE"))throw new IllegalArgumentException("Check quantity bounds and include UNSURE for guided enquiries");
  jdbc.update("update product_templates set name=?,description=?,active=?,min_quantity=?,max_quantity=?,gsm_options=?,sizes=?,finishes=?,revision=revision+1,updated_at=now() where code=?",e.name(),e.description(),e.active(),e.minQuantity(),e.maxQuantity(),json.writeValueAsString(e.gsmOptions()),json.writeValueAsString(e.sizes()),json.writeValueAsString(e.finishes()),code);
  Template next=get(code);jdbc.update("insert into template_activities(id,actor_id,code,revision,snapshot) values(?,?,?,?,?)",UUID.randomUUID(),actor.id(),code,next.revision(),snapshot(next));return next;
 }
 private Template read(java.sql.ResultSet rs)throws java.sql.SQLException{return new Template(rs.getString("code"),rs.getString("name"),rs.getString("description"),rs.getLong("revision"),rs.getBoolean("active"),rs.getInt("min_quantity"),rs.getInt("max_quantity"),json.readValue(rs.getString("gsm_options"),json.getTypeFactory().constructCollectionType(List.class,Integer.class)),json.readValue(rs.getString("sizes"),json.getTypeFactory().constructCollectionType(List.class,String.class)),json.readValue(rs.getString("finishes"),json.getTypeFactory().constructCollectionType(List.class,String.class)));}
}
