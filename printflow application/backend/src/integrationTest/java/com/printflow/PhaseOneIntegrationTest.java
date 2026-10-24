package com.printflow;

import com.printflow.identity.CustomerMail;
import com.printflow.identity.MfaSecrets;
import com.printflow.identity.Totp;
import com.printflow.enquiry.StaffEnquiryService.CreatedLead;
import com.printflow.enquiry.StaffEnquiryService.LeadView;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "server.address=127.0.0.1","app.staff.bootstrap.enabled=false","app.owner.bootstrap.enabled=false","app.demo.enabled=false",
 "APP_CUSTOMER_ACCOUNTS_ENABLED=true","APP_MAIL_ENABLED=false","APP_TURNSTILE_ENABLED=false",
 "APP_MAIL_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","APP_MFA_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
 "app.public-enquiries-enabled=true","server.servlet.session.cookie.secure=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PhaseOneIntegrationTest {
 @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;@Autowired JsonMapper json;@Autowired CustomerMail mail;@Autowired MfaSecrets secrets;
 UUID owner=UUID.randomUUID(),sales=UUID.randomUUID(),otherSales=UUID.randomUUID();String password=UUID.randomUUID().toString();
 String ownerName="it-p1-owner-"+owner,salesName="it-p1-sales-"+sales,otherName="it-p1-sales-"+otherSales;
 String firstEmail="it-customer-"+UUID.randomUUID()+"@example.test",secondEmail="it-customer-"+UUID.randomUUID()+"@example.test";
 Client ownerClient,salesClient,otherClient,first,second;String resetToken;UUID firstId;UUID leadId;String contactId;
 @BeforeAll void seed()throws Exception {
  for(var row:List.of(new Object[]{owner,ownerName,"OWNER"},new Object[]{sales,salesName,"SALES"},new Object[]{otherSales,otherName,"SALES"}))jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,?)",row[0],row[1],passwords.encode(password),row[2]);
  jdbc.update("update staff_users set contacts_access=true where id=?",sales);
  ownerClient=new Client();salesClient=new Client();otherClient=new Client();
  assertEquals(200,ownerClient.login(ownerName,password));assertEquals(200,salesClient.login(salesName,password));assertEquals(200,otherClient.login(otherName,password));
  String seed=Totp.secret();jdbc.update("update staff_users set totp_secret=? where id=?",secrets.encrypt(seed),owner);
  assertEquals(200,ownerClient.send("POST","/api/v1/auth/mfa/verify",json.writeValueAsString(Map.of("code",Totp.code(seed,Instant.now().getEpochSecond()/30))),true).statusCode());
 }
 @AfterAll void cleanup(){
  jdbc.update("update staff_users set enabled=false where id in (?,?,?) or username in (?,?)",owner,sales,otherSales,firstEmail,secondEmail);
  jdbc.update("update mail_outbox set payload=null,state='EXPIRED' where recipient in (?,?)",firstEmail,secondEmail);
 }
    class Client {
        CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        String token;
        HttpResponse<String> send(String method,String path,String body,boolean csrf)throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));
            if(csrf){csrf();builder.header("X-CSRF-TOKEN",token);}
            if(body!=null)builder.header("Content-Type","application/json");
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
        void csrf()throws Exception {
            var response=send("GET","/api/v1/auth/csrf",null,false);
            assertEquals(200,response.statusCode());
            token=(String)json.readValue(response.body(),Map.class).get("token");
        }
        int login(String name,String secret)throws Exception {
            csrf();String before=cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
            var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/auth/session"))
                .header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",token)
                .POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(name,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(secret,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==200){
                var cookie=cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow();
                assertTrue(cookie.isHttpOnly());assertTrue(!before.equals(cookie.getValue()),"Login must rotate the session identifier");
            }
            return response.statusCode();
        }
    }

 String body(Object input){return json.writeValueAsString(input);}
 Map<String,Object> registration(String email){return Map.of("name","Synthetic customer","email",email,"phone","9000000000","password",password,"consent",true,"privacyVersion","2026-10-23");}
 String latestToken(String email,String kind){String payload=jdbc.queryForObject("select payload from mail_outbox where recipient=? and kind=? and payload is not null order by created_at desc limit 1",String.class,email,kind);String content=mail.decryptForDelivery(payload);return content.split("#token=",2)[1].split("\\n",2)[0].trim();}
 Map<String,Object> enquiry(String email){return Map.of("customerName","Synthetic customer","email",email,"phone","9000000000","productCode","WEDDING_CARDS","quantity",250,"privacyConsent",true,"privacyVersion","2026-10-23");}
 @Test @Order(1) void registrationVerificationResetAndCustomerIsolation()throws Exception {
  first=new Client();second=new Client();
  assertEquals(403,first.send("POST","/api/public/identity/register",body(registration(firstEmail)),false).statusCode());
  assertEquals(202,first.send("POST","/api/public/identity/register",body(registration(firstEmail)),true).statusCode());
  assertEquals(401,first.login(firstEmail,password));String token=latestToken(firstEmail,"VERIFY");
  assertFalse(jdbc.queryForObject("select payload from mail_outbox where recipient=?",String.class,firstEmail).contains(token));
  assertEquals(202,first.send("POST","/api/public/identity/register",body(registration(firstEmail)),true).statusCode());
  assertEquals(200,first.send("POST","/api/public/identity/verify",body(Map.of("token",token)),true).statusCode());
  assertEquals(400,first.send("POST","/api/public/identity/verify",body(Map.of("token",token)),true).statusCode());assertEquals(200,first.login(firstEmail,password));
  firstId=jdbc.queryForObject("select id from staff_users where username=?",UUID.class,firstEmail);
  assertEquals(403,first.send("GET","/api/v1/staff/enquiries",null,false).statusCode());assertEquals(403,first.send("GET","/api/v1/owner/staff",null,false).statusCode());
  assertEquals(202,second.send("POST","/api/public/identity/register",body(registration(secondEmail)),true).statusCode());String secondToken=latestToken(secondEmail,"VERIFY");
  assertEquals(200,second.send("POST","/api/public/identity/verify",body(Map.of("token",secondToken)),true).statusCode());assertEquals(200,second.login(secondEmail,password));
  var created=first.send("POST","/api/v1/customer/enquiries",body(enquiry(secondEmail)),true);assertEquals(201,created.statusCode());
  UUID publicId=json.readValue(created.body(),com.printflow.enquiry.EnquiryDtos.CreatedEnquiry.class).publicId();leadId=jdbc.queryForObject("select id from enquiries where public_id=?",UUID.class,publicId);
  assertEquals(firstEmail,jdbc.queryForObject("select email from enquiries where id=?",String.class,leadId));
  assertTrue(first.send("GET","/api/v1/customer/enquiries",null,false).body().contains(leadId.toString()));
  assertEquals(404,second.send("GET","/api/v1/customer/enquiries/"+leadId,null,false).statusCode());
  var detail=first.send("GET","/api/v1/customer/enquiries/"+leadId,null,false);assertEquals(200,detail.statusCode());assertFalse(detail.body().contains("assigned_staff_id"));assertFalse(detail.body().contains("next_action"));
  assertEquals(202,first.send("POST","/api/public/identity/forgot",body(Map.of("email",firstEmail)),true).statusCode());resetToken=latestToken(firstEmail,"RESET");
  assertEquals(202,first.send("POST","/api/public/identity/forgot",body(Map.of("email","unknown-"+UUID.randomUUID()+"@example.test")),true).statusCode());
  String next=UUID.randomUUID().toString();assertEquals(200,first.send("POST","/api/public/identity/reset",body(Map.of("token",resetToken,"password",next)),true).statusCode());
  assertEquals(401,first.send("GET","/api/v1/customer/profile",null,false).statusCode());assertEquals(400,first.send("POST","/api/public/identity/reset",body(Map.of("token",resetToken,"password",password)),true).statusCode());assertEquals(200,first.login(firstEmail,next));
 }
 @Test @Order(2) void guestConsentTrackingClaimAndPrivacyRequests()throws Exception {
  Client guest=new Client();var invalid=new HashMap<>(enquiry(firstEmail));invalid.remove("privacyConsent");assertEquals(400,guest.send("POST","/api/public/enquiries",body(invalid),false).statusCode());
  invalid.put("privacyConsent",true);invalid.put("website","bot-input");assertEquals(400,guest.send("POST","/api/public/enquiries",body(invalid),false).statusCode());
  var created=guest.send("POST","/api/public/enquiries",body(enquiry(firstEmail)),false);assertEquals(201,created.statusCode());var link=json.readValue(created.body(),com.printflow.enquiry.EnquiryDtos.CreatedEnquiry.class);
  var claim=body(Map.of("publicId",link.publicId(),"trackingToken",link.trackingToken()));assertEquals(404,second.send("POST","/api/v1/customer/claim",claim,true).statusCode());assertEquals(200,first.send("POST","/api/v1/customer/claim",claim,true).statusCode());
  assertEquals(200,first.send("POST","/api/v1/customer/privacy","{\"kind\":\"EXPORT\"}",true).statusCode());assertEquals(409,first.send("POST","/api/v1/customer/privacy","{\"kind\":\"EXPORT\"}",true).statusCode());
  UUID request=jdbc.queryForObject("select id from privacy_requests where user_id=?",UUID.class,firstId);
  assertEquals(403,salesClient.send("GET","/api/v1/owner/privacy",null,false).statusCode());
  assertEquals(200,ownerClient.send("PATCH","/api/v1/owner/privacy/"+request,body(Map.of("status","RESOLVED","resolution","Synthetic export reviewed")),true).statusCode());assertTrue(first.send("GET","/api/v1/customer/privacy",null,false).body().contains("RESOLVED"));
 }
 @Test @Order(3) void crmContactIsolationDuplicateMergeAndLeadLink()throws Exception {
  Map<String,Object> input=Map.of("name","CRM Synthetic","email","crm-"+owner+"@example.test","phone","9000000000","organization","Synthetic Hotel","address","Fictional address","permissionToContact",true,"consentNote","Synthetic explicit permission");
  var created=salesClient.send("POST","/api/v1/staff/contacts",body(input),true);assertEquals(200,created.statusCode());contactId=(String)json.readValue(created.body(),Map.class).get("id");
  assertFalse(otherClient.send("GET","/api/v1/staff/contacts",null,false).body().contains(contactId));assertTrue(ownerClient.send("GET","/api/v1/staff/contacts",null,false).body().contains(contactId));
  var edit=new HashMap<>(input);edit.put("revision",0);assertEquals(404,otherClient.send("PUT","/api/v1/staff/contacts/"+contactId,body(edit),true).statusCode());assertEquals(200,salesClient.send("PUT","/api/v1/staff/contacts/"+contactId,body(edit),true).statusCode());assertEquals(409,salesClient.send("PUT","/api/v1/staff/contacts/"+contactId,body(edit),true).statusCode());
  var duplicate=salesClient.send("POST","/api/v1/staff/contacts",body(input),true);String target=(String)json.readValue(duplicate.body(),Map.class).get("id");
  var matches=salesClient.send("GET","/api/v1/staff/contacts/duplicates?email="+URLEncoder.encode((String)input.get("email"),StandardCharsets.UTF_8)+"&phone=9000000000",null,false);assertTrue(matches.body().contains(contactId));assertTrue(matches.body().contains(target));
  long revision=jdbc.queryForObject("select revision from enquiries where id=?",Long.class,leadId);
  assertEquals(200,salesClient.send("POST","/api/v1/staff/contacts/"+contactId+"/enquiries/"+leadId,body(Map.of("revision",revision)),true).statusCode());
  assertEquals(200,salesClient.send("POST","/api/v1/staff/contacts/"+contactId+"/merge",body(Map.of("targetId",target,"revision",1,"targetRevision",0)),true).statusCode());assertEquals(UUID.fromString(target),jdbc.queryForObject("select contact_id from enquiries where id=?",UUID.class,leadId));
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from contact_activities where actor_id=?",sales));
 }
 @Test @Order(4) void templatesSnapshotAndGuestBodyLimits()throws Exception {
  String snapshot=jdbc.queryForObject("select template_snapshot from enquiries where id=?",String.class,leadId);assertNotNull(snapshot);
  var template=ownerClient.send("GET","/api/public/templates/WEDDING_CARDS",null,false);var t=json.readValue(template.body(),Map.class);long version=((Number)t.get("revision")).longValue();
  assertEquals(403,salesClient.send("PUT","/api/v1/owner/templates/WEDDING_CARDS",body(t),true).statusCode());
  assertEquals(200,ownerClient.send("PUT","/api/v1/owner/templates/WEDDING_CARDS",body(t),true).statusCode());assertEquals(409,ownerClient.send("PUT","/api/v1/owner/templates/WEDDING_CARDS",body(t),true).statusCode());
  assertEquals(snapshot,jdbc.queryForObject("select template_snapshot from enquiries where id=?",String.class,leadId));assertEquals(version,jdbc.queryForObject("select template_revision from enquiries where id=?",Long.class,leadId));
  assertEquals(413,new Client().send("POST","/api/public/enquiries","x".repeat(131073),false).statusCode());
  assertEquals(200,first.send("GET","/api/public/identity/status",null,false).statusCode());
 }
}
