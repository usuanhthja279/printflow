package com.printflow.crm;
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
@RequestMapping("/api/v1/staff/contacts")
public class ContactsController {
 public record Contact(UUID id,UUID ownerId,String name,String email,String phone,String organization,String address,boolean permissionToContact,String consentNote,long revision) {}
 public record Input(@NotBlank @Size(max=140) String name,@NotBlank @Email @Size(max=254) String email,@NotBlank @Pattern(regexp="[+0-9() .-]{7,32}") String phone,@NotNull @Size(max=160) String organization,@NotNull @Size(max=1000) String address,boolean permissionToContact,@NotNull @Size(max=500) String consentNote,Long revision) {}
 public record Merge(@NotNull UUID targetId,@NotNull Long revision,@NotNull Long targetRevision) {}
 public record Link(@NotNull Long revision) {}
 private final JdbcTemplate jdbc;private final com.printflow.identity.StaffPermissions permissions;
 public ContactsController(JdbcTemplate jdbc,com.printflow.identity.StaffPermissions permissions){this.jdbc=jdbc;this.permissions=permissions;}
 @GetMapping public ResponseEntity<List<Contact>> list(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="") String search,@RequestParam(required=false) UUID before){
  permissions.requireContacts(actor);if(search.length()>140)throw new IllegalArgumentException("Search is too long");String term="%"+search.toLowerCase(Locale.ROOT)+"%";
  return response(jdbc.query("select * from crm_contacts c where merged_into is null and (? or owner_id=?) and (lower(name) like ? or lower(email) like ? or phone like ? or lower(organization) like ?) and (?::uuid is null or (created_at,id)<(select created_at,id from crm_contacts where id=?)) order by created_at desc,id desc limit 50",(rs,n)->read(rs),owner(actor),actor.id(),term,term,term,term,before,before));
 }
 @GetMapping("/duplicates") public ResponseEntity<List<Contact>> duplicates(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam String email,@RequestParam String phone){
  permissions.requireContacts(actor);if(email.length()>254||phone.length()>32)throw new IllegalArgumentException("Invalid contact search");
  return response(jdbc.query("select * from crm_contacts where merged_into is null and (? or owner_id=?) and (lower(email)=? or regexp_replace(phone,'[^0-9]','','g')=?) order by created_at,id limit 20",(rs,n)->read(rs),owner(actor),actor.id(),email.trim().toLowerCase(Locale.ROOT),phone.replaceAll("[^0-9]","")));
 }
 @PostMapping @Transactional public ResponseEntity<Contact> create(@AuthenticationPrincipal StaffPrincipal actor,@Valid @RequestBody Input input){
  permissions.requireContacts(actor);validate(input);UUID id=UUID.randomUUID();jdbc.update("insert into crm_contacts(id,owner_id,name,email,phone,organization,address,permission_to_contact,consent_note) values(?,?,?,?,?,?,?,?,?)",id,actor.id(),input.name().trim(),input.email().trim().toLowerCase(Locale.ROOT),input.phone(),input.organization(),input.address(),input.permissionToContact(),input.consentNote());audit(actor,id,"CONTACT_CREATED");return response(get(actor,id));
 }
 @PutMapping("/{id}") @Transactional public ResponseEntity<Contact> edit(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id,@Valid @RequestBody Input input){
  permissions.requireContacts(actor);validate(input);lock(actor,id);Contact old=get(actor,id);if(input.revision()==null||old.revision()!=input.revision())throw new ResponseStatusException(HttpStatus.CONFLICT,"Contact changed; refresh");
  jdbc.update("update crm_contacts set name=?,email=?,phone=?,organization=?,address=?,permission_to_contact=?,consent_note=?,revision=revision+1 where id=?",input.name(),input.email().trim().toLowerCase(Locale.ROOT),input.phone(),input.organization(),input.address(),input.permissionToContact(),input.consentNote(),id);audit(actor,id,"CONTACT_UPDATED");return response(get(actor,id));
 }
 @PostMapping("/{id}/merge") @Transactional public ResponseEntity<Contact> merge(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id,@Valid @RequestBody Merge input){
  permissions.requireContacts(actor);if(id.equals(input.targetId()))throw new IllegalArgumentException("Choose two different contacts");
  // Consistent lock ordering prevents two opposite merge requests deadlocking.
  for(UUID next:java.util.stream.Stream.of(id,input.targetId()).sorted().toList())lock(actor,next);
  Contact source=get(actor,id),target=get(actor,input.targetId());
  if(source.revision()!=input.revision()||target.revision()!=input.targetRevision()||!source.ownerId().equals(target.ownerId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Merge requires current records with the same responsible staff");
  jdbc.update("update enquiries set contact_id=?,revision=revision+1 where contact_id=?",target.id(),source.id());
  jdbc.update("update crm_contacts set merged_into=?,revision=revision+1 where id=?",target.id(),source.id());jdbc.update("update crm_contacts set revision=revision+1 where id=?",target.id());audit(actor,id,"CONTACT_MERGED");audit(actor,target.id(),"MERGE_RECEIVED");return response(get(actor,target.id()));
 }
 @PostMapping("/{id}/enquiries/{lead}") @Transactional public ResponseEntity<Map<String,String>> link(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id,@PathVariable UUID lead,@Valid @RequestBody Link input){
  permissions.requireContacts(actor);get(actor,id);var rows=jdbc.queryForList("select assigned_staff_id,designer_id,status,revision from enquiries where id=? for update",lead);
  if(rows.isEmpty()||!owner(actor)&&("NEW".equals(rows.getFirst().get("status"))||!(actor.role().equals("SALES")&&actor.id().equals(rows.getFirst().get("assigned_staff_id"))||actor.role().equals("DESIGNER")&&actor.id().equals(rows.getFirst().get("designer_id")))))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(((Number)rows.getFirst().get("revision")).longValue()!=input.revision())throw new ResponseStatusException(HttpStatus.CONFLICT);
  jdbc.update("update enquiries set contact_id=?,revision=revision+1 where id=?",id,lead);jdbc.update("insert into lead_activities(id,enquiry_id,actor_id,action,details) values(?,?,?,'CONTACT_LINKED',?)",UUID.randomUUID(),lead,actor.id(),id.toString());return response(Map.of("code","CONTACT_LINKED"));
 }
 private void lock(StaffPrincipal actor,UUID id){jdbc.queryForList("select id from crm_contacts where id=? for update",id);get(actor,id);}
 private Contact get(StaffPrincipal actor,UUID id){return jdbc.query("select * from crm_contacts where id=? and merged_into is null and (? or owner_id=?)",(rs,n)->read(rs),id,owner(actor),actor.id()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private Contact read(java.sql.ResultSet rs)throws java.sql.SQLException{return new Contact(rs.getObject("id",UUID.class),rs.getObject("owner_id",UUID.class),rs.getString("name"),rs.getString("email"),rs.getString("phone"),rs.getString("organization"),rs.getString("address"),rs.getBoolean("permission_to_contact"),rs.getString("consent_note"),rs.getLong("revision"));}
 private boolean owner(StaffPrincipal actor){return actor.role().equals("OWNER");}
 private void validate(Input input){if(input.permissionToContact()&&input.consentNote().isBlank())throw new IllegalArgumentException("Record when and how contact permission was given");}
 private void audit(StaffPrincipal actor,UUID id,String action){jdbc.update("insert into contact_activities(id,actor_id,contact_id,action) values(?,?,?,?)",UUID.randomUUID(),actor.id(),id,action);}
 private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
