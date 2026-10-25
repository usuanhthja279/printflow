package com.printflow.customer;
import com.printflow.enquiry.*;
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
@RequestMapping("/api/v1/customer")
public class CustomerWorkspaceController {
 public record Claim(@NotNull UUID publicId,@NotBlank @Size(max=100) String trackingToken) {}
 public record Profile(@NotBlank @Size(max=140) String name,@NotBlank @Pattern(regexp="[+0-9() .-]{7,32}") String phone,@NotNull @Size(max=1000) String address,@NotNull @Size(max=160) String organization) {}
 private final JdbcTemplate jdbc;private final EnquiryService enquiries;
 public CustomerWorkspaceController(JdbcTemplate jdbc,EnquiryService enquiries){this.jdbc=jdbc;this.enquiries=enquiries;}
 @GetMapping("/profile") public ResponseEntity<Map<String,Object>> profile(@AuthenticationPrincipal StaffPrincipal user){return response(jdbc.queryForMap("select p.name,p.phone,p.address,p.organization,u.username as email,p.privacy_version from customer_profiles p join staff_users u on u.id=p.user_id where p.user_id=? and p.email_verified",user.id()));}
 @PutMapping("/profile") public ResponseEntity<Map<String,String>> edit(@AuthenticationPrincipal StaffPrincipal user,@Valid @RequestBody Profile p){jdbc.update("update customer_profiles set name=?,phone=?,address=?,organization=? where user_id=?",p.name(),p.phone(),p.address(),p.organization(),user.id());return response(Map.of("code","PROFILE_SAVED"));}
 @GetMapping("/enquiries") public ResponseEntity<List<Map<String,Object>>> list(@AuthenticationPrincipal StaffPrincipal user,@RequestParam(required=false) UUID before){return response(jdbc.queryForList("select case when status='CLOSED' then 'Closed' when status='NEW' then 'Pending review at business' when exists(select 1 from quote_versions q where q.enquiry_id=enquiries.id and q.state='ISSUED') then 'Review your quotation' when status in ('REVIEWING','QUOTED') then 'Pending quotation at business' else 'Design and fulfilment' end as customer_stage,id,public_id,product_code,quantity,status,created_at,template_revision,closure_kind,closure_message,exists(select 1 from quote_versions q where q.enquiry_id=enquiries.id and q.state='ACCEPTED') as live_work from enquiries where customer_user_id=? and (?::uuid is null or ((status='CLOSED')::int,-extract(epoch from created_at),id)>(select (status='CLOSED')::int,-extract(epoch from created_at),id from enquiries where id=? and customer_user_id=?)) order by (status='CLOSED') asc,created_at desc,id asc limit 50",user.id(),before,before,user.id()));}
 @GetMapping("/enquiries/{id}") public ResponseEntity<Map<String,Object>> detail(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID id){
  var rows=jdbc.queryForList("select case when status='CLOSED' then 'Closed' when status='NEW' then 'Pending review at business' when exists(select 1 from quote_versions q where q.enquiry_id=enquiries.id and q.state='ISSUED') then 'Review your quotation' when status in ('REVIEWING','QUOTED') then 'Pending quotation at business' else 'Design and fulfilment' end as customer_stage,id,public_id,product_code,quantity,intended_use,size,paper_gsm,print_sides,finish,notes,status,created_at,template_snapshot,closure_kind,closure_message,to_char(requested_date,'YYYY-MM-DD') as requested_date,exists(select 1 from quote_versions q where q.enquiry_id=enquiries.id and q.state='ACCEPTED') as live_work from enquiries where id=? and customer_user_id=?",id,user.id());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var data=rows.getFirst();data.put("events",jdbc.queryForList("select to_status as status,created_at from enquiry_events where enquiry_id=? order by created_at,id",id));return response(data);
 }
 @PostMapping("/enquiries") @Transactional public ResponseEntity<EnquiryDtos.CreatedEnquiry> create(@AuthenticationPrincipal StaffPrincipal user,@Valid @RequestBody EnquiryDtos.CreateEnquiry input,@RequestHeader(value="Idempotency-Key",required=false) String idempotencyKey){
  if(!Boolean.TRUE.equals(input.privacyConsent())||!com.printflow.identity.CustomerIdentityController.PRIVACY.equals(input.privacyVersion()))throw new IllegalArgumentException("Review the privacy notice");
  var profile=jdbc.queryForMap("select name,phone from customer_profiles where user_id=? and email_verified",user.id());
  var request=new EnquiryDtos.CreateEnquiry((String)profile.get("name"),user.username(),(String)profile.get("phone"),input.productCode(),input.quantity(),input.size(),input.paperGsm(),input.printSides(),input.finish(),input.notes(),input.privacyConsent(),input.privacyVersion(),null,null,input.requestedDate(),input.intendedUse(),input.whatsappConsent());
  var result=enquiries.create(request,idempotencyKey);jdbc.update("update enquiries set customer_user_id=? where public_id=? and customer_user_id is null",user.id(),result.publicId());return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(result);
 }
 @PostMapping("/claim") @Transactional public ResponseEntity<Map<String,String>> claim(@AuthenticationPrincipal StaffPrincipal user,@Valid @RequestBody Claim input){
  var rows=jdbc.queryForList("select id,email,tracking_token_sha256,customer_user_id from enquiries where public_id=? for update",input.publicId());
  if(rows.isEmpty()||!user.username().equalsIgnoreCase((String)rows.getFirst().get("email"))||!TokenUtil.matches(input.trackingToken(),(String)rows.getFirst().get("tracking_token_sha256"))||rows.getFirst().get("customer_user_id")!=null&&!user.id().equals(rows.getFirst().get("customer_user_id")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  jdbc.update("update enquiries set customer_user_id=?,revision=revision+1 where public_id=?",user.id(),input.publicId());return response(Map.of("code","ENQUIRY_LINKED"));
 }
 private <T> ResponseEntity<T> response(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
}
