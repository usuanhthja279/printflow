package com.printflow;

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
    "server.address=127.0.0.1","app.staff.bootstrap.enabled=false","app.demo.enabled=false",
    "app.public-enquiries-enabled=false","server.servlet.session.cookie.secure=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StaffWorkflowIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JsonMapper json;
    final UUID first=UUID.randomUUID(),second=UUID.randomUUID(),operator=UUID.randomUUID();
    final String password=UUID.randomUUID().toString();
    String firstName="it-a-"+first,secondName="it-b-"+second,operatorName="it-op-"+operator;
    Client owner,other;
    CreatedLead created;
    @BeforeAll void seedFictionalStaff(){
        for(var user:List.of(new Object[]{first,firstName,"SALES"},new Object[]{second,secondName,"SALES"},new Object[]{operator,operatorName,"OPERATOR"}))
            jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,?)",user[0],user[1],passwords.encode(password),user[2]);
    }
    @AfterAll void disableTestAccounts(){
        jdbc.update("update staff_users set enabled=false where id in (?,?,?)",first,second,operator);
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
    @Test @Order(1) void identityAndDefaultDeny()throws Exception {
        var anonymous=new Client();
        assertEquals(401,anonymous.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
        assertEquals(403,anonymous.send("POST","/api/v1/auth/session","{}",false).statusCode());
        assertEquals(404,anonymous.send("GET","/api/demo/enquiries",null,false).statusCode());
        assertEquals(401,anonymous.login(firstName,"incorrect-password"));
        owner=new Client();assertEquals(200,owner.login(firstName,password));
        var session=owner.send("GET","/api/v1/auth/session",null,false);
        assertEquals(200,session.statusCode());assertFalse(session.body().contains("password"));
        assertTrue(jdbc.queryForObject("select password_hash from staff_users where id=?",String.class,first).startsWith("$2a$12$"));
        var restricted=new Client();assertEquals(200,restricted.login(operatorName,password));
        assertEquals(403,restricted.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
        assertEquals(403,restricted.send("POST","/api/v1/staff/enquiries","{}",true).statusCode());
    }
    @Test @Order(2) void staffAssistedLeadAndCsrf()throws Exception {
        String body="{\"enquiry\":{\"customerName\":\"Synthetic Integration Customer\",\"email\":\"integration@example.test\",\"phone\":\"9000000000\",\"productCode\":\"WEDDING_CARDS\",\"quantity\":250},\"source\":\"PHONE\"}";
        assertEquals(403,owner.send("POST","/api/v1/staff/enquiries",body,false).statusCode());
        var response=owner.send("POST","/api/v1/staff/enquiries",body,true);
        assertEquals(201,response.statusCode());created=json.readValue(response.body(),CreatedLead.class);
        assertEquals(first,created.lead().assignedStaffId());assertEquals("PHONE",created.lead().source());
        assertFalse(created.trackingToken().isBlank());jdbc.update("update enquiries set status='REVIEWING' where id=?",created.lead().id());try{var byReference=owner.send("GET","/api/v1/staff/enquiries?search=PF-"+created.lead().publicId().toString().replace("-","").substring(0,8),null,false);assertEquals(200,byReference.statusCode());assertTrue(byReference.body().contains(created.lead().id().toString()),byReference.body());var plainSearch=owner.send("GET","/api/v1/staff/enquiries?search="+created.lead().publicId().toString().replace("-","").substring(0,8),null,false);assertFalse(plainSearch.body().contains(created.lead().id().toString()),plainSearch.body());}finally{jdbc.update("update enquiries set status='REVIEWING' where id=?",created.lead().id());}
    }
    @Test @Order(3) void ownershipAndConcurrentRevisionProtection()throws Exception {
        other=new Client();assertEquals(200,other.login(secondName,password));
        String base="/api/v1/staff/enquiries/"+created.lead().id();
        assertFalse(other.send("GET","/api/v1/staff/enquiries",null,false).body().contains(created.lead().id().toString()));
        assertEquals(404,other.send("GET",base+"/activities",null,false).statusCode());
        String change="{\"revision\":"+created.lead().revision()+",\"priority\":\"HIGH\",\"nextAction\":\"Confirm synthetic specifications\",\"followUpAt\":\"2026-10-24T10:00:00Z\"}";
        assertEquals(404,other.send("PATCH",base+"/follow-up",change,true).statusCode());
        assertEquals(200,owner.send("PATCH",base+"/follow-up",change,true).statusCode());
        assertEquals(409,owner.send("PATCH",base+"/follow-up",change,true).statusCode());
        assertEquals(2,jdbc.queryForObject("select count(*) from lead_activities where enquiry_id=?",Integer.class,created.lead().id()));
        var saved=jdbc.queryForMap("select next_action,follow_up_at,priority from enquiries where id=?",created.lead().id());
        assertEquals("HIGH",saved.get("priority"));assertEquals("Confirm synthetic specifications",saved.get("next_action"));
        var publicResponse=new Client().client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/public/enquiries/"+created.lead().publicId())).header("X-Tracking-Token",created.trackingToken()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,publicResponse.statusCode());assertFalse(publicResponse.body().contains("nextAction"));assertFalse(publicResponse.body().contains("example.test"));
    }
    @Test @Order(4) void assignmentAndStatusPreserveAuditAndPublicTimeline()throws Exception {
        String base="/api/v1/staff/enquiries/"+created.lead().id();
        long revision=jdbc.queryForObject("select revision from enquiries where id=?",Long.class,created.lead().id());
        assertEquals(200,owner.send("PATCH",base+"/assignment","{\"revision\":"+revision+",\"claim\":false}",true).statusCode());
        revision=jdbc.queryForObject("select revision from enquiries where id=?",Long.class,created.lead().id());
        assertEquals(200,other.send("PATCH",base+"/assignment","{\"revision\":"+revision+",\"claim\":true}",true).statusCode());
        assertEquals(404,owner.send("GET",base+"/activities",null,false).statusCode());
        revision=jdbc.queryForObject("select revision from enquiries where id=?",Long.class,created.lead().id());
        assertEquals(200,other.send("POST",base+"/resolve","{\"revision\":"+revision+",\"kind\":\"REJECTED\",\"message\":\"Unable to fulfill the requested specification after review.\"}",true).statusCode());
        assertNull(jdbc.queryForObject("select follow_up_at from enquiries where id=?",java.sql.Timestamp.class,created.lead().id()));
        assertEquals(2,jdbc.queryForObject("select count(*) from enquiry_events where enquiry_id=?",Integer.class,created.lead().id()));
        assertEquals(5,jdbc.queryForObject("select count(*) from lead_activities where enquiry_id=?",Integer.class,created.lead().id()));
    }
    @Test @Order(5) void disabledAccountsAndLogoutInvalidateSessions()throws Exception {
        jdbc.update("update staff_users set enabled=false where id=?",first);
        assertEquals(401,owner.send("GET","/api/v1/auth/session",null,false).statusCode());
        assertEquals(200,other.send("POST","/api/v1/auth/logout",null,true).statusCode());
        assertEquals(401,other.send("GET","/api/v1/staff/enquiries",null,false).statusCode());
    }
    @Test @Order(6) void repeatedLoginAttemptsAreRateLimited()throws Exception {
        Client attacker=new Client();String unknown="unknown-"+UUID.randomUUID();
        for(int i=0;i<8;i++)assertEquals(401,attacker.login(unknown,"incorrect-password"));
        assertEquals(429,attacker.login(unknown,"incorrect-password"));
    }
}
