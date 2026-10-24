package com.printflow.quote;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Quotations {
 public record Issue(@Min(0) int expectedVersion,@NotBlank @Size(max=3000) String scope,
  @Min(1) @Max(100000000000L) long totalPaise,@NotBlank @Size(max=2000) String terms,
  LocalDate requestedDate,@NotNull LocalDate proposedDate,Boolean acceptedOffline,@Pattern(regexp="PHONE|EMAIL") String acceptanceSource,@Size(max=1000) String acceptanceEvidence,
  @Size(max=30) List<@NotNull @Valid Line> lines,UUID amendmentOf,@Size(max=1000) String amendmentReason,Boolean requestArtwork) {
 public Issue(int v,String s,long total,String terms,LocalDate requested,LocalDate proposed,Boolean offline,String source,String evidence,List<Line> lines,UUID amendment,String reason){this(v,s,total,terms,requested,proposed,offline,source,evidence,lines,amendment,reason,false);}
 }
 public record Line(@NotBlank @Size(max=500) String description,@Min(1) @Max(1000000) int quantity,@Min(0) @Max(100000000000L) long unitPaise,@Min(0) @Max(10000) int taxBps,Map<String,String> specifications){}
 public record Draft(@Min(0) int expectedRevision,@NotNull Map<String,Object> payload){}
 public record Reason(@NotBlank @Size(min=20,max=1000) String reason){}
 public record Decision(@NotBlank @Pattern(regexp="ACCEPT|REQUEST_CHANGES") String action,@NotNull @Size(max=1000) String note) {}
 private final com.printflow.files.DesignerRouting routing;private final com.printflow.identity.CustomerSetup setup;private final com.printflow.identity.ProjectNotifications notifications;private final JdbcTemplate db;private final tools.jackson.databind.json.JsonMapper json;private final QuotationPdf pdf;private final com.printflow.catalog.PrintVocabulary vocabulary;private final com.printflow.identity.CustomerMail mail;
 public Quotations(JdbcTemplate db,tools.jackson.databind.json.JsonMapper json,QuotationPdf pdf,com.printflow.catalog.PrintVocabulary vocabulary,com.printflow.identity.CustomerMail mail,com.printflow.identity.CustomerSetup setup,com.printflow.identity.ProjectNotifications notifications,com.printflow.files.DesignerRouting routing){this.routing=routing;this.setup=setup;this.notifications=notifications;this.mail=mail;this.db=db;this.json=json;this.pdf=pdf;this.vocabulary=vocabulary;}
 private LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Kolkata"));}
 private Map<String,Object> lead(StaffPrincipal user,UUID id,boolean lock,boolean write){
  if(!Set.of("OWNER","SALES","CUSTOMER").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  var rows=db.queryForList("select id,assigned_staff_id,customer_user_id,status,requested_date from enquiries where id=?"+(lock?" for update":""),id);
  if(rows.isEmpty())throw new NoSuchElementException();var e=rows.getFirst();
  if(user.role().equals("CUSTOMER")){
   if(!user.id().equals(e.get("customer_user_id")))throw new NoSuchElementException();
  }else if(!user.role().equals("OWNER")&&("NEW".equals(e.get("status"))||!user.id().equals(e.get("assigned_staff_id"))))throw new NoSuchElementException();
  if(write){
   if("CLOSED".equals(e.get("status")))throw conflict("This enquiry is closed");
   if(user.role().equals("SALES")&&!user.id().equals(e.get("assigned_staff_id")))throw conflict("Claim this enquiry before issuing a quote");
  }
  return e;
 }
 private ResponseStatusException conflict(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
 private static final String FIELDS="id,version,scope,total_paise,terms,to_char(requested_date,'YYYY-MM-DD') as requested_date,to_char(proposed_date,'YYYY-MM-DD') as proposed_date,acceptance_source,acceptance_evidence,specification_snapshot::text as specification_snapshot,issued_at,state,responded_at,response_note,line_items::text as line_items,amendment_of,amendment_reason,withdrawal_reason,document_sha256";
 @Transactional(readOnly=true) public List<Map<String,Object>> list(StaffPrincipal user,UUID leadId){lead(user,leadId,false,false);return db.queryForList("select "+FIELDS+" from quote_versions where enquiry_id=? order by version desc limit 100",leadId);}
 @Transactional public Map<String,Object> issue(StaffPrincipal user,UUID leadId,Issue input){
  if(!Set.of("OWNER","SALES").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  var enquiry=lead(user,leadId,true,true);
  if(db.queryForObject("select exists(select 1 from fulfilment_work where enquiry_id=?)",Boolean.class,leadId))throw conflict("Production has been released; a reviewed hold/change process is required before amending this agreement");
  if(db.queryForObject("select exists(select 1 from finance_invoices where enquiry_id=?)",Boolean.class,leadId))throw conflict("An invoice has been issued; invoiced agreements require a credit-note process before amendment");
  if("NEW".equals(enquiry.get("status")))throw conflict("Accept the enquiry for review before issuing a quotation");
  if(input.proposedDate().isBefore(today()))throw new IllegalArgumentException("Proposed date must be today or later");
  boolean offline=Boolean.TRUE.equals(input.acceptedOffline());
  if(offline&&(input.acceptanceSource()==null||input.acceptanceEvidence()==null||input.acceptanceEvidence().trim().length()<20))throw new IllegalArgumentException("Record phone/email agreement details (at least 20 characters)");
  LocalDate requested=enquiry.get("requested_date")!=null?((java.sql.Date)enquiry.get("requested_date")).toLocalDate():input.requestedDate()!=null?input.requestedDate():today();
  var latest=db.queryForList("select version,state from quote_versions where enquiry_id=? order by version desc limit 1",leadId);
  int version=latest.isEmpty()?0:((Number)latest.getFirst().get("version")).intValue();
  if(!latest.isEmpty()&&"ISSUED".equals(latest.getFirst().get("state")))throw conflict("Withdraw the outstanding quotation or wait for customer changes before revising");
  if(version!=input.expectedVersion())throw conflict("A newer quote exists; refresh before issuing");
  var accepted=db.queryForList("select id from quote_versions where enquiry_id=? and state='ACCEPTED'",leadId);
  UUID currentAccepted=accepted.isEmpty()?null:(UUID)accepted.getFirst().get("id");
  if(currentAccepted!=null&&(!currentAccepted.equals(input.amendmentOf())||input.amendmentReason()==null||input.amendmentReason().trim().length()<20))throw conflict("An explicit amendment and explanation are required for accepted work");
  if(currentAccepted==null&&input.amendmentOf()!=null)throw conflict("The accepted agreement changed; refresh first");
  var lines=pricedLines(input);
  db.update("update quote_versions set state='SUPERSEDED' where enquiry_id=? and state='ISSUED'",leadId);
  UUID id=UUID.randomUUID();
  String snapshot=db.queryForObject("select jsonb_build_object('productCode',product_code,'quantity',quantity,'size',size,'paperGsm',paper_gsm,'printSides',print_sides,'finish',finish,'notes',notes,'template',template_snapshot)::text from enquiries where id=?",String.class,leadId);
  String customer=db.queryForObject("select customer_name from enquiries where id=?",String.class,leadId);
  byte[] document=pdf.create(id,leadId,version+1,customer,input,requested,lines,snapshot);
  db.update("insert into quote_versions(id,enquiry_id,version,scope,total_paise,terms,requested_date,proposed_date,specification_snapshot,issued_by,line_items,amendment_of,amendment_reason,document_pdf,document_sha256) values(?,?,?,?,?,?,?,?,?::jsonb,?,?::jsonb,?,?,?,?)",id,leadId,version+1,input.scope().trim(),input.totalPaise(),input.terms().trim(),requested,input.proposedDate(),snapshot,user.id(),json.writeValueAsString(lines),currentAccepted,currentAccepted==null?null:input.amendmentReason().trim(),document,hash(document));
  audit(id,user,"ISSUED");
  if(offline){replaceAccepted(leadId,id,user);db.update("update quote_versions set state='ACCEPTED',responded_at=now(),acceptance_source=?,acceptance_evidence=?,acceptance_recorded_by=? where id=?",input.acceptanceSource(),input.acceptanceEvidence().trim(),user.id(),id);audit(id,user,"OFFLINE_ACCEPTED");}
  if(Boolean.TRUE.equals(input.requestArtwork()))requestArtwork(user,leadId);
  db.update("delete from quote_drafts where enquiry_id=?",leadId);
  status(leadId,user,offline||currentAccepted!=null?"IN_PROGRESS":"QUOTED",offline?"Quotation accepted via "+input.acceptanceSource()+" (recorded by staff)":currentAccepted!=null?"Amended quotation issued; previous agreement remains in effect until acceptance":"Quotation issued for customer review");
  String access=setup.prepare(leadId);
  if(offline)routing.allocate(leadId);
  var recipient=db.queryForMap("select email,public_id,customer_user_id from enquiries where id=?",leadId);
  mail.queueProject((UUID)recipient.get("customer_user_id"),leadId,recipient.get("email").toString(),recipient.get("public_id").toString(),"QUOTE",id,"Quotation v"+(version+1)+(offline?" records your phone/email agreement.":" is in review and waiting for your acceptance.")+" Review the scope, total, dates and terms in your customer workspace.\n\n"+access);
  return db.queryForMap("select "+FIELDS+" from quote_versions where id=?",id);
 }
 @Transactional public Map<String,Object> decide(StaffPrincipal user,UUID leadId,UUID quoteId,Decision input){
  if(!"CUSTOMER".equals(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  lead(user,leadId,true,true);
  var rows=db.queryForList("select "+FIELDS+" from quote_versions where id=? and enquiry_id=?",quoteId,leadId);
  if(rows.isEmpty())throw new NoSuchElementException();var q=rows.getFirst();String state=(String)q.get("state");
  String target=input.action().equals("ACCEPT")?"ACCEPTED":"CHANGES_REQUESTED";
  if(target.equals(state))return q;
  if(!"ISSUED".equals(state))throw conflict("Only the current issued quote can receive a response");
  if(target.equals("CHANGES_REQUESTED")&&input.note().isBlank())throw new IllegalArgumentException("Describe the changes you need");
  if(target.equals("ACCEPTED"))replaceAccepted(leadId,quoteId,user);
  db.update("update quote_versions set state=?,responded_at=now(),response_note=?,acceptance_source=?,acceptance_recorded_by=? where id=?",target,input.note().trim(),target.equals("ACCEPTED")?"PORTAL":null,target.equals("ACCEPTED")?user.id():null,quoteId);
  if(target.equals("ACCEPTED")){status(leadId,user,"IN_PROGRESS","Customer accepted the quotation");routing.allocate(leadId);}
  audit(quoteId,user,target);notifications.both(leadId,target.equals("ACCEPTED")?"QUOTE_ACCEPT":"QUOTE_CHANGE",quoteId,target.equals("ACCEPTED")?"The customer accepted quotation v"+q.get("version")+". This work is now live; artwork and proof approval come next.":"The customer requested quotation changes. Review the customer workspace response.");
  return db.queryForMap("select "+FIELDS+" from quote_versions where id=?",quoteId);
 }
 @Transactional public Map<String,Object> requestArtwork(StaffPrincipal user,UUID id){if(!Set.of("OWNER","SALES").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);lead(user,id,true,true);
  if(db.queryForObject("select exists(select 1 from design_artifacts where enquiry_id=? and kind='REFERENCE' and state='CLEAN')",Boolean.class,id))return Map.of("code","ARTWORK_ALREADY_RECEIVED");
  if(db.queryForObject("select exists(select 1 from work_events where enquiry_id=? and kind='ARTWORK_REQUEST' and created_at>now()-interval '1 hour')",Boolean.class,id))return Map.of("code","ALREADY_REQUESTED");
  UUID event=UUID.randomUUID();String text="Please share your reference artwork with J.D.Printex. Reply to this email or contact our team if you need help; signed-in customers can upload it from their enquiry or live work.";
  db.update("insert into work_events(id,enquiry_id,actor_id,kind,message) values(?,?,?,'ARTWORK_REQUEST',?)",event,id,user.id(),text);
  var e=db.queryForMap("select customer_user_id,email,public_id from enquiries where id=?",id);String notification=mail.queueProject((UUID)e.get("customer_user_id"),id,e.get("email").toString(),e.get("public_id").toString(),"ARTWORK",event,text);return Map.of("code","ARTWORK_REQUESTED","notification",notification,"deliveryEnabled",mail.deliveryEnabled());
 }
 private void replaceAccepted(UUID lead,UUID replacement,StaffPrincipal user){for(var old:db.queryForList("select id from quote_versions where enquiry_id=? and state='ACCEPTED' and id<>?",lead,replacement))audit((UUID)old.get("id"),user,"AGREEMENT_REPLACED");db.update("update quote_versions set state='REPLACED' where enquiry_id=? and state='ACCEPTED' and id<>?",lead,replacement);}
 public List<Map<String,Object>> pricedLines(Issue input){
  List<Line> source=input.lines()==null||input.lines().isEmpty()?List.of(new Line(input.scope().substring(0,Math.min(input.scope().length(),500)),1,input.totalPaise(),0,Map.of("pricingBasis","ALL_INCLUSIVE_LOT"))):input.lines();
  List<Map<String,Object>> result=new ArrayList<>();long sum=0;
  for(Line l:source){long base=Math.multiplyExact((long)l.quantity(),l.unitPaise());if(base>100000000000L)throw new IllegalArgumentException("Line amount exceeds quotation limit");long tax=(base*l.taxBps()+5000)/10000,total=base+tax;sum+=total;
   var specs=l.specifications()==null?Map.<String,String>of():l.specifications();if(specs.size()>12||specs.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null||e.getKey().length()>60||e.getValue().length()>500))throw new IllegalArgumentException("Print specifications are too long");
   vocabulary.validate(specs);result.add(Map.of("description",l.description().trim(),"quantity",l.quantity(),"unitPaise",l.unitPaise(),"taxBps",l.taxBps(),"subtotalPaise",base,"taxPaise",tax,"totalPaise",total,"specifications",specs));
  }if(sum!=input.totalPaise())throw new IllegalArgumentException("Quotation total must match item totals including explicitly entered tax");return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> draft(StaffPrincipal user,UUID id){if(!Set.of("OWNER","SALES").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);lead(user,id,false,false);var rows=db.queryForList("select revision,payload::text as payload,updated_at from quote_drafts where enquiry_id=?",id);return rows.isEmpty()?Map.of("revision",0,"payload","{}"):rows.getFirst();}
 @Transactional public Map<String,Object> saveDraft(StaffPrincipal user,UUID id,Draft input){if(!Set.of("OWNER","SALES").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);lead(user,id,true,true);String payload=json.writeValueAsString(input.payload());if(payload.length()>30000)throw new IllegalArgumentException("Draft too large");var old=draft(user,id);int revision=((Number)old.get("revision")).intValue();if(revision!=input.expectedRevision())throw conflict("Draft changed; reload before saving");db.update("insert into quote_drafts(enquiry_id,revision,payload,updated_by) values(?,?,?::jsonb,?) on conflict(enquiry_id) do update set revision=excluded.revision,payload=excluded.payload,updated_by=excluded.updated_by,updated_at=now()",id,revision+1,payload,user.id());return draft(user,id);}
 @Transactional public Map<String,Object> withdraw(StaffPrincipal user,UUID leadId,UUID quoteId,Reason input){if(!Set.of("OWNER","SALES").contains(user.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);lead(user,leadId,true,true);if(input.reason().trim().length()<20)throw new IllegalArgumentException("Explain the withdrawal");var rows=db.queryForList("select state from quote_versions where id=? and enquiry_id=?",quoteId,leadId);if(rows.isEmpty())throw new NoSuchElementException();if(!"ISSUED".equals(rows.getFirst().get("state")))throw conflict("Only an outstanding offer can be withdrawn");db.update("update quote_versions set state='WITHDRAWN',withdrawal_reason=? where id=?",input.reason().trim(),quoteId);audit(quoteId,user,"WITHDRAWN");boolean accepted=db.queryForObject("select exists(select 1 from quote_versions where enquiry_id=? and state='ACCEPTED')",Boolean.class,leadId);status(leadId,user,accepted?"IN_PROGRESS":"REVIEWING","Quotation withdrawn: "+input.reason().trim());return Map.of("code","WITHDRAWN");}
 @Transactional(readOnly=true) public byte[] document(StaffPrincipal user,UUID leadId,UUID quoteId){lead(user,leadId,false,false);var rows=db.queryForList("select document_pdf,document_sha256 from quote_versions where id=? and enquiry_id=?",quoteId,leadId);if(rows.isEmpty()||rows.getFirst().get("document_pdf")==null)throw new NoSuchElementException();byte[] bytes=(byte[])rows.getFirst().get("document_pdf");if(!hash(bytes).equals(rows.getFirst().get("document_sha256")))throw conflict("Quotation document integrity failed");return bytes;}
 private String hash(byte[] bytes){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void status(UUID lead,StaffPrincipal user,String next,String message){String previous=db.queryForObject("select status from enquiries where id=?",String.class,lead);db.update("update enquiries set status=?,revision=revision+1,updated_at=now() where id=?",next,lead);if(!next.equals(previous))db.update("insert into enquiry_events(id,enquiry_id,event_type,from_status,to_status) values(?,?,'QUOTATION_WORKFLOW',?,?)",UUID.randomUUID(),lead,previous,next);db.update("insert into work_events(id,enquiry_id,actor_id,kind,message) values(?,?,?,'QUOTATION',?)",UUID.randomUUID(),lead,user.id(),message.substring(0,Math.min(message.length(),1000)));}
 private void audit(UUID quote,StaffPrincipal actor,String action){db.update("insert into quote_events(id,quote_id,actor_id,action) values(?,?,?,?)",UUID.randomUUID(),quote,actor.id(),action);}
}
