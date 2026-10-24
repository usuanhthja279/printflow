package com.printflow;

import com.printflow.identity.Totp;
import com.printflow.identity.MfaSecrets;
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
 "APP_MFA_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","server.servlet.session.cookie.secure=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OwnerAccessIntegrationTest {
 @LocalServerPort int port;
 @Autowired JdbcTemplate jdbc;
 @Autowired PasswordEncoder passwords;
 @Autowired JsonMapper json;
 @Autowired MfaSecrets secrets;
 UUID owner=UUID.randomUUID(),sales=UUID.randomUUID();
 String ownerName="it-owner-"+owner,salesName="it-sales-"+sales,password=UUID.randomUUID().toString();
 @BeforeAll void seed(){for(var row:List.of(new Object[]{owner,ownerName,"OWNER"},new Object[]{sales,salesName,"SALES"}))jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,?)",row[0],row[1],passwords.encode(password),row[2]);}
 @AfterAll void cleanup(){jdbc.update("update staff_users set enabled=false where id in (?,?)",owner,sales);jdbc.update("update staff_users set enabled=false where username like ?","it-managed-"+owner+"%");}
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

 @Test void ownerEnrollmentAdministrationAndRevocation()throws Exception {
  Client c=new Client(),s=new Client();assertEquals(200,c.login(ownerName,password));assertEquals(200,s.login(salesName,password));
  assertEquals(403,s.send("GET","/api/v1/owner/staff",null,false).statusCode());
  assertEquals(403,c.send("GET","/api/v1/owner/staff",null,false).statusCode());
  assertEquals(403,c.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
  var enrollment=c.send("POST","/api/v1/auth/mfa/enroll",null,true);assertEquals(200,enrollment.statusCode());
  String seed=(String)json.readValue(enrollment.body(),Map.class).get("secret");
  String code=Totp.code(seed,Instant.now().getEpochSecond()/30);
  var confirmation=c.send("POST","/api/v1/auth/mfa/confirm",json.writeValueAsString(Map.of("code",code)),true);assertEquals(200,confirmation.statusCode());
  var codes=(List<String>)json.readValue(confirmation.body(),Map.class).get("recoveryCodes");assertEquals(8,codes.size());
  String stored=jdbc.queryForObject("select totp_secret from staff_users where id=?",String.class,owner);assertNotEquals(seed,stored);assertEquals(seed,secrets.decrypt(stored));
  assertEquals(200,c.send("GET","/api/v1/owner/staff",null,false).statusCode());
  assertEquals(200,c.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
  assertEquals(409,c.send("PATCH","/api/v1/owner/staff/"+owner+"/access","{\"role\":\"SALES\",\"enabled\":false}",true).statusCode());
  String name="it-managed-"+owner;
  var created=c.send("POST","/api/v1/owner/staff",json.writeValueAsString(Map.of("username",name,"role","SALES","temporaryPassword",password)),true);
  assertEquals(200,created.statusCode());String id=(String)json.readValue(created.body(),Map.class).get("id");
  Client managed=new Client();assertEquals(200,managed.login(name,password));assertEquals(403,managed.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
  String nextPassword=UUID.randomUUID().toString();
  assertEquals(200,managed.send("POST","/api/v1/auth/password",json.writeValueAsString(Map.of("currentPassword",password,"newPassword",nextPassword)),true).statusCode());
  assertEquals(401,managed.send("GET","/api/v1/auth/session",null,false).statusCode());assertEquals(200,managed.login(name,nextPassword));
  assertEquals(200,managed.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
  assertEquals(200,c.send("PATCH","/api/v1/owner/staff/"+id+"/access","{\"role\":\"OPERATOR\",\"enabled\":true}",true).statusCode());
  assertEquals(401,managed.send("GET","/api/v1/auth/session",null,false).statusCode());
  assertEquals(200,c.send("POST","/api/v1/owner/staff/"+id+"/password",json.writeValueAsString(Map.of("password",password)),true).statusCode());
  assertEquals(200,managed.login(name,password));
  assertEquals(200,c.send("PATCH","/api/v1/owner/staff/"+id+"/access","{\"role\":\"OPERATOR\",\"enabled\":false}",true).statusCode());
  assertEquals(401,managed.send("GET","/api/v1/auth/session",null,false).statusCode());
  var leadResponse=s.send("POST","/api/v1/staff/enquiries","{\"source\":\"PHONE\",\"enquiry\":{\"customerName\":\"Owner assignment synthetic\",\"email\":\"owner-assignment@example.test\",\"phone\":\"9000000000\",\"productCode\":\"HOTEL_PRINT\",\"quantity\":50}}",true);
  assertEquals(201,leadResponse.statusCode());var lead=json.readValue(leadResponse.body(),CreatedLead.class).lead();
  assertTrue(c.send("GET","/api/v1/staff/enquiries",null,false).body().contains(lead.id().toString()));
  var assignment=json.writeValueAsString(Map.of("revision",lead.revision(),"staffId",owner));
  assertEquals(403,s.send("PATCH","/api/v1/owner/enquiries/"+lead.id()+"/assignment",assignment,true).statusCode());
  assertEquals(400,c.send("PATCH","/api/v1/owner/enquiries/"+lead.id()+"/assignment",json.writeValueAsString(Map.of("revision",lead.revision(),"staffId",id)),true).statusCode());
  assertEquals(200,c.send("PATCH","/api/v1/owner/enquiries/"+lead.id()+"/assignment",assignment,true).statusCode());
  assertEquals(409,c.send("PATCH","/api/v1/owner/enquiries/"+lead.id()+"/assignment",assignment,true).statusCode());
  assertFalse(s.send("GET","/api/v1/staff/enquiries",null,false).body().contains(lead.id().toString()));
  Client challenge=new Client();assertEquals(200,challenge.login(ownerName,password));
  assertEquals(401,challenge.send("POST","/api/v1/auth/mfa/verify",json.writeValueAsString(Map.of("code",code)),true).statusCode(),"TOTP cannot be reused");
  assertEquals(200,challenge.send("POST","/api/v1/auth/mfa/verify",json.writeValueAsString(Map.of("code",codes.get(0))),true).statusCode());
  assertEquals(401,challenge.send("POST","/api/v1/auth/mfa/verify",json.writeValueAsString(Map.of("code",codes.get(0))),true).statusCode());
  assertEquals(403,c.send("POST","/api/v1/owner/staff","{}",false).statusCode());
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from security_activities where actor_id=?",owner));
  var listed=c.send("GET","/api/v1/owner/staff",null,false);assertFalse(listed.body().contains("password_hash"));assertFalse(listed.body().contains(seed));
  assertTrue(jdbc.queryForObject("select count(*) from security_activities where actor_id=?",Integer.class,owner)>=6);
  // MFA throttling is persistent and independent of password-login attempts.
  Client limited=new Client();assertEquals(200,limited.login(ownerName,password));
  for(int i=0;i<5;i++)assertEquals(401,limited.send("POST","/api/v1/auth/mfa/verify","{\"code\":\"bad\"}",true).statusCode());
  assertEquals(429,limited.send("POST","/api/v1/auth/mfa/verify","{\"code\":\"bad\"}",true).statusCode());
 }
}
