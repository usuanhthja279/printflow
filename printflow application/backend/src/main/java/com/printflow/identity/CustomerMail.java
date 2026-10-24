package com.printflow.identity;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerMail {
 private final JdbcTemplate jdbc;private final MfaSecrets encryption;
 private final boolean enabled;private final String host,username,password,base;private final int port;private final java.util.Set<String> allowedRecipients;private final java.time.Instant sendAfter;private final String ownerAddress;
 public CustomerMail(JdbcTemplate jdbc,@Value("${APP_MAIL_ENCRYPTION_KEY:}") String key,
  @Value("${APP_MAIL_ENABLED:false}") boolean enabled,@Value("${APP_MAIL_HOST:smtp.gmail.com}") String host,
  @Value("${APP_MAIL_PORT:587}") int port,@Value("${APP_MAIL_USERNAME:}") String username,
  @Value("${APP_MAIL_PASSWORD:}") String password,@Value("${APP_PUBLIC_BASE_URL:http://127.0.0.1:3000}") String base,
  @Value("${APP_MAIL_ALLOWED_RECIPIENTS:}") String allowed,@Value("${APP_MAIL_SEND_AFTER:}") String after,org.springframework.core.env.Environment environment){
  this.jdbc=jdbc;this.encryption=new MfaSecrets(key);this.enabled=enabled;this.host=host;this.port=port;this.username=credential(environment,"SPRING_MAIL_USERNAME",null,username);this.password=credential(environment,"SPRING_MAIL_PASSWORD","SPRING_MAIL_PASSWROD",password);this.base=base;
  this.ownerAddress=environment.getProperty("APP_OWNER_NOTIFICATION_EMAIL",this.username);
  this.allowedRecipients=Arrays.stream(allowed.split(",")).map(String::trim).map(v->v.toLowerCase(Locale.ROOT)).filter(v->!v.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
  this.sendAfter=after.isBlank()?null:java.time.Instant.parse(after);
  boolean nativeDev=environment.matchesProfiles("dev")&&!environment.matchesProfiles("prod");
  boolean loopback=base.equals("http://127.0.0.1:3000")||base.equals("http://localhost:3000");
  if(enabled&&(!encryption.configured()||this.username.isBlank()||this.password.isBlank()||port!=587||(!base.startsWith("https://")&&!(nativeDev&&loopback))||(nativeDev&&(allowedRecipients.isEmpty()||sendAfter==null))))throw new IllegalStateException("Email requires private credentials, encryption key, STARTTLS 587 and HTTPS; native DEV additionally requires explicit recipients and an activation timestamp, and may use exact loopback HTTP");

 }
 private static String credential(org.springframework.core.env.Environment env,String primary,String alias,String fallback){String value=env.getProperty(primary);if(value==null||value.isBlank())value=alias==null?null:env.getProperty(alias);return value==null||value.isBlank()?fallback:value;}
 public String ownerNotificationAddress(){return ownerAddress;}
 public String publicBase(){return base;}
 public boolean deliveryEnabled(){return enabled;}
 public boolean available(){return encryption.configured();}
 public void queue(UUID user,String recipient,String kind,String token){
  String path=kind.equals("VERIFY")?"verify":"reset";
  String body="J.D.Printex\n\n"+(kind.equals("VERIFY")?"Verify your email":"Reset your password")+":\n"+base+"/customer?mode="+path+"#token="+token+"\n\nThis single-use link expires in 30 minutes. Ignore it if you did not request it.";
  jdbc.update("insert into mail_outbox(id,user_id,recipient,kind,payload) values(?,?,?,?,?)",UUID.randomUUID(),user,recipient,kind,encryption.encrypt(body));
 }
 @Transactional
 public String queueRejection(UUID user,UUID enquiry,String recipient,String reference,String explanation){
  return queueProject(user,enquiry,recipient,reference,"REJECTION",enquiry,explanation);
 }
 @Transactional
 public String queueProject(UUID user,UUID enquiry,String recipient,String reference,String kind,UUID event,String text){return queueProject(user,enquiry,recipient,reference,kind,event,text,null);}
 @Transactional
 public String queueProject(UUID user,UUID enquiry,String recipient,String reference,String kind,UUID event,String text,UUID feedback){return queueProject(user,enquiry,recipient,reference,kind,event,text,feedback,false);}
 @Transactional
 public String queueProject(UUID user,UUID enquiry,String recipient,String reference,String kind,UUID event,String text,UUID feedback,boolean team){
  // Serialize phase roots for this enquiry; retries keep the same event Message-ID.
  jdbc.queryForObject("select id from enquiries where id=? for update",UUID.class,enquiry);
  recipient=recipient.trim().toLowerCase(Locale.ROOT);
  String phase=MailPresentation.phase(kind),id=kind.equals("ENQUIRY")?"<enquiry-"+enquiry+"@jdprintex.in>":"<"+kind.toLowerCase(Locale.ROOT)+"-"+event+"@jdprintex.in>";
  var prior=jdbc.queryForList("select message_id,thread_root_id from mail_outbox where enquiry_id=? and lower(recipient)=? and thread_phase=? and message_id is not null order by created_at desc,id desc limit 1",enquiry,recipient,phase);
  String previous=prior.isEmpty()?null:prior.getFirst().get("message_id").toString();
  String root=prior.isEmpty()?id:prior.getFirst().get("thread_root_id").toString();
  String body="J.D.Printex\n\nEnquiry reference: "+reference+"\n\n"+text+(team?"\n\nTeam workspace: "+base+"/business\nSign in with your staff account to review this enquiry.":"\n\nCustomer workspace: "+base+"/customer\nPlease sign in with the email used for your enquiry. Contact our team if you need help.");
  String product=jdbc.queryForObject("select coalesce(t.name,e.product_code) from enquiries e left join product_templates t on t.code=e.product_code where e.id=?",String.class,enquiry);
  jdbc.update("insert into mail_outbox(id,user_id,enquiry_id,recipient,kind,payload,state,message_id,reply_to_id,subject,quote_id,thread_phase,thread_root_id,feedback_id,is_team) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) on conflict do nothing",UUID.randomUUID(),user,enquiry,recipient,kind,available()?encryption.encrypt(body):null,available()?"PENDING":"FAILED",id,previous,MailPresentation.subject(kind,reference,product),kind.equals("QUOTE")?event:null,phase,root,feedback,team);
  return available()?"QUEUED":"NOT_CONFIGURED";
 }
 /** Used only by test fixtures; never expose decrypted mail via a public route. */
 public String decryptForDelivery(String value){return encryption.decrypt(value);}
 /** Assemble without network I/O so MIME alternatives, immutable attachments and headers can be verified. */
 jakarta.mail.internet.MimeMessage compose(Map<String,Object> row)throws Exception{
  String kind=row.get("kind").toString(),body=encryption.decrypt(row.get("payload").toString()),summary=null;
  byte[] attachment=null;String filename=null;
  if(kind.equals("QUOTE")){
   if(row.get("quote_id")==null)throw new IllegalStateException("Quotation email has no issued version");
   var q=jdbc.queryForMap("select q.document_pdf,q.document_sha256,q.version,q.total_paise,to_char(q.proposed_date,'DD Mon YYYY') as delivery,e.public_id from quote_versions q join enquiries e on e.id=q.enquiry_id where q.id=? and q.enquiry_id=?",row.get("quote_id"),row.get("enquiry_id"));
   attachment=(byte[])q.get("document_pdf");
   if(attachment==null||attachment.length==0||attachment.length>20*1024*1024||!java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(attachment)).equals(q.get("document_sha256")))throw new IllegalStateException("Issued quotation attachment integrity failed");
   filename="J.D.Printex-Quotation-v"+q.get("version")+"-"+MailPresentation.reference(q.get("public_id").toString())+".pdf";
   summary="Quotation v"+q.get("version")+" · "+java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN")).format(new java.math.BigDecimal(q.get("total_paise").toString()).movePointLeft(2))+"\nProposed delivery / collection: "+q.get("delivery");
   body+="\n\nAttached: "+filename;
  }
  if(row.get("feedback_id")!=null){var review=jdbc.queryForMap("select review_pdf,review_sha256 from reference_feedback where id=? and enquiry_id=?",row.get("feedback_id"),row.get("enquiry_id"));attachment=(byte[])review.get("review_pdf");if(attachment==null||attachment.length>15*1024*1024||!java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(attachment)).equals(review.get("review_sha256")))throw new IllegalStateException("Review attachment integrity failed");filename="J.D.Printex-Artwork-review.pdf";body+="\n\nAttached: "+filename;}
  if(kind.equals("INVOICE")){if(row.get("invoice_id")==null)throw new IllegalStateException("Invoice mail has no immutable invoice");var invoice=jdbc.queryForMap("select document_pdf,document_sha256,invoice_number from finance_invoices where id=? and enquiry_id=?",row.get("invoice_id"),row.get("enquiry_id"));attachment=(byte[])invoice.get("document_pdf");if(attachment==null||attachment.length>20*1024*1024||!com.printflow.customer.Payments.sha(attachment).equals(invoice.get("document_sha256")))throw new IllegalStateException("Invoice integrity failed");filename="J.D.Printex-Invoice-INV-"+invoice.get("invoice_number")+".pdf";}
  var message=new JavaMailSenderImpl().createMimeMessage();var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,true,"UTF-8");
  helper.setFrom(username,"J.D.Printex");helper.setTo(row.get("recipient").toString());helper.setSubject(row.get("subject")==null?MailPresentation.subject(kind,null,null):row.get("subject").toString());helper.setText(body,MailPresentation.html(kind,body,base,summary,Boolean.TRUE.equals(row.get("is_team"))));
  if(attachment!=null)helper.addAttachment(filename,new org.springframework.core.io.ByteArrayResource(attachment),"application/pdf");
  if(row.get("reply_to_id")!=null){message.setHeader("In-Reply-To",row.get("reply_to_id").toString());String root=row.get("thread_root_id")==null?"<enquiry-"+row.get("enquiry_id")+"@jdprintex.in>":row.get("thread_root_id").toString();message.setHeader("References",root.equals(row.get("reply_to_id"))?root:root+" "+row.get("reply_to_id"));}
  message.saveChanges();if(row.get("message_id")!=null)message.setHeader("Message-ID",row.get("message_id").toString());return message;
 }
 @Scheduled(fixedDelay=30000) @Transactional
 public void dispatch(){
  jdbc.update("delete from request_limits where window_start<now()-interval '1 day'");
  jdbc.update("update mail_outbox set payload=null,state=case when state='PENDING' then 'EXPIRED' else state end where payload is not null and ((enquiry_id is not null and created_at<now()-interval '1 day') or (enquiry_id is null and created_at<now()-interval '30 minutes'))");
  if(!enabled)return;
  // Do not send historical cleanup/test messages when enabling delivery locally.
  if(sendAfter!=null)jdbc.update("update mail_outbox set state='HELD' where state='PENDING' and created_at<?",java.sql.Timestamp.from(sendAfter));
  var rows=jdbc.queryForList("select id,recipient,kind,payload,message_id,reply_to_id,subject,enquiry_id,quote_id,thread_root_id,feedback_id,is_team,invoice_id from mail_outbox where state='PENDING' and attempts<5 and payload is not null and ((enquiry_id is not null and created_at>now()-interval '1 day') or (enquiry_id is null and created_at>now()-interval '25 minutes')) order by created_at limit 10 for update skip locked");
  for(var row:rows){
   String recipient=row.get("recipient").toString().trim().toLowerCase(Locale.ROOT);
   if(recipient.endsWith(".test")||recipient.endsWith(".invalid")||(!allowedRecipients.isEmpty()&&!allowedRecipients.contains("*")&&!allowedRecipients.contains(recipient))){jdbc.update("update mail_outbox set state='HELD' where id=?",row.get("id"));continue;}
   try{
    var sender=new JavaMailSenderImpl();sender.setHost(host);sender.setPort(port);sender.setUsername(username);sender.setPassword(password);
    var props=sender.getJavaMailProperties();props.setProperty("mail.smtp.auth","true");props.setProperty("mail.smtp.starttls.enable","true");props.setProperty("mail.smtp.starttls.required","true");props.setProperty("mail.smtp.ssl.checkserveridentity","true");props.setProperty("mail.smtp.connectiontimeout","5000");props.setProperty("mail.smtp.timeout","5000");props.setProperty("mail.smtp.writetimeout","5000");
    sender.send(compose(row));
    jdbc.update("update mail_outbox set state='SENT',payload=null,sent_at=now(),failure_code=null,attempts=attempts+1 where id=?",row.get("id"));
   }catch(Exception ignored){jdbc.update("update mail_outbox set attempts=attempts+1,failure_code='SMTP_SEND_FAILED',state=case when attempts>=4 then 'FAILED' else 'PENDING' end where id=?",row.get("id"));}
  }
  jdbc.update("update mail_outbox set state='EXPIRED',payload=null where state='PENDING' and ((enquiry_id is not null and created_at<now()-interval '1 day') or (enquiry_id is null and created_at<now()-interval '30 minutes'))");
  jdbc.update("delete from request_limits where window_start<now()-interval '1 day'");
 }
}
