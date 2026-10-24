package com.printflow;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","app.staff.bootstrap.enabled=false","app.owner.bootstrap.enabled=false","APP_MAIL_ENABLED=false","app.demo.enabled=false","server.servlet.session.cookie.secure=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class QuotationIntegrationTest {
 @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;@Autowired JsonMapper json;
 UUID sales=UUID.randomUUID(),otherSales=UUID.randomUUID(),customer=UUID.randomUUID(),otherCustomer=UUID.randomUUID(),operator=UUID.randomUUID();
 String password=UUID.randomUUID().toString();Client seller,buyer,stranger,otherSeller,worker;
 @BeforeAll void seed()throws Exception{
  var users=List.of(sales,otherSales,customer,otherCustomer,operator);var roles=List.of("SALES","SALES","CUSTOMER","CUSTOMER","OPERATOR");
  for(int i=0;i<users.size();i++)jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,?)",users.get(i),"it-quote-"+users.get(i)+"@example.test",passwords.encode(password),roles.get(i));
  seller=login(sales);buyer=login(customer);stranger=login(otherCustomer);otherSeller=login(otherSales);worker=login(operator);
 }
 Client login(UUID id)throws Exception{var c=new Client();assertEquals(200,c.login("it-quote-"+id+"@example.test",password));return c;}
 @AfterAll void cleanup(){for(UUID id:List.of(sales,otherSales,customer,otherCustomer,operator))jdbc.update("update staff_users set enabled=false where id=?",id);}
 UUID lead(){UUID id=UUID.randomUUID();jdbc.update("insert into enquiries(id,public_id,tracking_token_sha256,customer_name,email,phone,product_code,quantity,status,assigned_staff_id,customer_user_id) values(?,?,?,'Synthetic quote customer','quote@example.test','9000000000','WEDDING_CARDS',250,'REVIEWING',?,?)",id,UUID.randomUUID(),"0".repeat(64),sales,customer);return id;}
 String path(UUID id){return "/api/v1/staff/enquiries/"+id+"/quotes";}
 String customerPath(UUID id){return "/api/v1/customer/enquiries/"+id+"/quotes";}
 String issue(int version,long amount){var today=LocalDate.now(ZoneId.of("Asia/Kolkata"));return json.writeValueAsString(Map.of("expectedVersion",version,"scope","250 cards including envelopes","totalPaise",amount,"terms","All inclusive. Advance agreed separately.","proposedDate",today.plusDays(10).toString(),"validUntil",today.plusDays(3).toString()));}
 Map<?,?> data(HttpResponse<String> r){return json.readValue(r.body(),Map.class);}
 String decision(UUID lead,Object quote){return customerPath(lead)+"/"+quote+"/decision";}
 @Test void exactVersionAcceptanceAndImmutableHistory()throws Exception{
  UUID lead=lead();var first=seller.send("POST",path(lead),issue(0,123456),true);assertEquals(201,first.statusCode(),first.body());Object firstId=data(first).get("id");
  assertEquals(409,seller.send("POST",path(lead),issue(0,999),true).statusCode());
  assertEquals(409,seller.send("POST",path(lead),issue(1,234567),true).statusCode());
  assertEquals(200,seller.send("POST",path(lead)+"/"+firstId+"/withdraw","{\"reason\":\"Customer requirements need a revised offer\"}",true).statusCode());
  var second=seller.send("POST",path(lead),issue(1,234567),true);assertEquals(201,second.statusCode(),second.body());Object id=data(second).get("id");
  assertEquals(409,buyer.send("POST",decision(lead,firstId),"{\"action\":\"ACCEPT\",\"note\":\"\"}",true).statusCode());
  assertEquals(200,buyer.send("POST",decision(lead,id),"{\"action\":\"ACCEPT\",\"note\":\"Agreed\"}",true).statusCode());
  assertEquals(200,buyer.send("POST",decision(lead,id),"{\"action\":\"ACCEPT\",\"note\":\"Agreed\"}",true).statusCode());
  assertEquals(1,jdbc.queryForObject("select count(*) from quote_events where quote_id=? and action='ACCEPTED'",Integer.class,UUID.fromString(id.toString())));
  assertEquals(409,seller.send("POST",path(lead),issue(2,300000),true).statusCode());
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update quote_versions set total_paise=1 where id=?",UUID.fromString(id.toString())));
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from quote_events where quote_id=?",UUID.fromString(id.toString())));
  var history=buyer.send("GET",customerPath(lead),null,false);assertEquals(200,history.statusCode());assertTrue(history.body().contains("WITHDRAWN"));assertTrue(history.body().contains("234567"));assertFalse(history.body().contains("assigned_staff_id"));
 }
 @Test void assignmentIsolationCsrfAndValidation()throws Exception{
  UUID lead=lead();assertEquals(401,new Client().send("GET",path(lead),null,false).statusCode());
  assertEquals(403,buyer.send("POST",path(lead),issue(0,100),true).statusCode());
  assertEquals(403,worker.send("GET",path(lead),null,false).statusCode());
  assertEquals(404,otherSeller.send("GET",path(lead),null,false).statusCode());assertEquals(404,stranger.send("GET",customerPath(lead),null,false).statusCode());
  assertEquals(403,seller.send("POST",path(lead),issue(0,100),false).statusCode());
  assertEquals(400,seller.send("POST",path(lead),issue(0,0),true).statusCode());
  jdbc.update("update enquiries set assigned_staff_id=null where id=?",lead);assertEquals(404,seller.send("POST",path(lead),issue(0,100),true).statusCode());
 }
 @Test void changeRequestAndExpiredOffer()throws Exception{
  UUID lead=lead();var issued=seller.send("POST",path(lead),issue(0,50000),true);Object id=data(issued).get("id");
  assertEquals(400,buyer.send("POST",decision(lead,id),"{\"action\":\"REQUEST_CHANGES\",\"note\":\"\"}",true).statusCode());
  assertEquals(200,buyer.send("POST",decision(lead,id),"{\"action\":\"REQUEST_CHANGES\",\"note\":\"Need a later date\"}",true).statusCode());
  assertEquals(409,buyer.send("POST",decision(lead,id),"{\"action\":\"ACCEPT\",\"note\":\"\"}",true).statusCode());
  assertEquals(201,seller.send("POST",path(lead),issue(1,60000),true).statusCode());
  UUID expiredLead=lead(),expired=UUID.randomUUID();
  jdbc.update("insert into quote_versions(id,enquiry_id,version,scope,total_paise,terms,proposed_date,valid_until,specification_snapshot,issued_by) values(?,?,1,'Expired fixture',100,'Synthetic terms',current_date+10,current_date-1,'{}'::jsonb,?)",expired,expiredLead,sales);
  assertEquals(200,buyer.send("POST",decision(expiredLead,expired),"{\"action\":\"ACCEPT\",\"note\":\"\"}",true).statusCode());
 }
 @Test void concurrentIssuanceHasOneWinner()throws Exception{
  UUID lead=lead();Client concurrent=login(sales);String body=issue(0,10000);
  try(var pool=Executors.newFixedThreadPool(2)){
   var a=pool.submit(()->seller.send("POST",path(lead),body,true).statusCode());var b=pool.submit(()->concurrent.send("POST",path(lead),body,true).statusCode());
   var statuses=new ArrayList<>(List.of(a.get(),b.get()));Collections.sort(statuses);assertEquals(List.of(201,409),statuses);
  }
 }
 @Test void draftItemPricingPdfOrderAndControlledAmendment()throws Exception{
  UUID lead=lead();String draft="/api/v1/staff/enquiries/"+lead+"/quote-draft";
  String saved="{\"expectedRevision\":0,\"payload\":{\"scope\":\"Partial draft\"}}";
  assertEquals(200,seller.send("PUT",draft,saved,true).statusCode());assertEquals(409,seller.send("PUT",draft,saved,true).statusCode());assertEquals(403,buyer.send("GET",draft,null,false).statusCode());assertEquals(404,otherSeller.send("GET",draft,null,false).statusCode());
  var body=new HashMap<String,Object>(json.readValue(issue(0,123000),Map.class));body.put("lines",List.of(Map.of("description","Wedding cards","quantity",100,"unitPaise",1000,"taxBps",1800,"specifications",Map.of("paper","250 GSM","finish","Gloss")),Map.of("description","Envelopes","quantity",100,"unitPaise",50,"taxBps",0,"specifications",Map.of())));
  var realLines=body.get("lines");body.put("lines",List.of(Map.of("description","Invalid option","quantity",1,"unitPaise",123000,"taxBps",0,"specifications",Map.of("paperType","NOT_CONFIGURED"))));assertEquals(400,seller.send("POST",path(lead),json.writeValueAsString(body),true).statusCode());body.put("lines",realLines);
  body.put("totalPaise",1);assertEquals(400,seller.send("POST",path(lead),json.writeValueAsString(body),true).statusCode());body.put("totalPaise",123000);
  var issued=seller.send("POST",path(lead),json.writeValueAsString(body),true);assertEquals(201,issued.statusCode(),issued.body());UUID first=UUID.fromString(data(issued).get("id").toString());
  assertEquals(0,((Number)data(seller.send("GET",draft,null,false)).get("revision")).intValue());
  String pdfPath=customerPath(lead)+"/"+first+"/document";var document=buyer.client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+pdfPath)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,document.statusCode());assertEquals("application/pdf",document.headers().firstValue("Content-Type").orElseThrow());byte[] bytes=document.body();
  try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("J.D.Printex"));assertTrue(text.contains("1230.00"));assertTrue(text.contains("Wedding cards"));assertTrue(text.contains(first.toString()));assertFalse(text.contains("Valid until"));}
  java.nio.file.Files.createDirectories(java.nio.file.Path.of("..",".local"));java.nio.file.Files.write(java.nio.file.Path.of("..",".local","phase2-quotation.pdf"),bytes);
  assertEquals(404,stranger.send("GET",pdfPath,null,false).statusCode());assertEquals(200,buyer.send("POST",decision(lead,first),"{\"action\":\"ACCEPT\",\"note\":\"Approved\"}",true).statusCode());
  String orders="/api/v1/staff/enquiries/"+lead+"/orders",convert=json.writeValueAsString(Map.of("quoteId",first));var order=seller.send("POST",orders,convert,true);assertEquals(200,order.statusCode(),order.body());assertEquals(true,data(order).get("created"));assertEquals(false,data(seller.send("POST",orders,convert,true)).get("created"));assertEquals(403,buyer.send("POST",orders,convert,true).statusCode());assertEquals(404,otherSeller.send("GET",orders,null,false).statusCode());
  UUID orderId=UUID.fromString(data(order).get("id").toString());assertEquals(2,jdbc.queryForObject("select count(*) from commercial_order_lines where order_id=?",Integer.class,orderId));assertEquals(123000L,jdbc.queryForObject("select sum(total_paise) from commercial_order_lines where order_id=?",Long.class,orderId));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update commercial_orders set total_paise=1 where id=?",orderId));
  var amendment=new HashMap<String,Object>(json.readValue(issue(1,150000),Map.class));amendment.put("amendmentOf",first);amendment.put("amendmentReason","Customer requested a larger agreed print run");var second=seller.send("POST",path(lead),json.writeValueAsString(amendment),true);assertEquals(201,second.statusCode(),second.body());UUID secondId=UUID.fromString(data(second).get("id").toString());assertEquals("ACCEPTED",jdbc.queryForObject("select state from quote_versions where id=?",String.class,first));
  assertEquals(200,buyer.send("POST",decision(lead,secondId),"{\"action\":\"ACCEPT\",\"note\":\"Amendment approved\"}",true).statusCode());assertEquals("REPLACED",jdbc.queryForObject("select state from quote_versions where id=?",String.class,first));assertEquals("PORTAL",jdbc.queryForObject("select acceptance_source from quote_versions where id=?",String.class,first));
  assertArrayEquals(bytes,(byte[])jdbc.queryForMap("select document_pdf from quote_versions where id=?",first).get("document_pdf"));assertEquals(200,seller.send("POST",orders,json.writeValueAsString(Map.of("quoteId",secondId)),true).statusCode());assertEquals(2,jdbc.queryForObject("select count(*) from commercial_orders where enquiry_id=?",Integer.class,lead));assertEquals(409,seller.send("POST",orders,convert,true).statusCode());assertTrue(jdbc.queryForObject("select specifications::text from commercial_order_lines where order_id=(select id from commercial_orders where quote_id=?)",String.class,secondId).contains("ALL_INCLUSIVE_LOT"));
 }
 @Test void withdrawOfferKeepsEvidenceAndConcurrentConversionIsIdempotent()throws Exception{
  UUID lead=lead();var issued=seller.send("POST",path(lead),issue(0,10000),true);Object quote=data(issued).get("id");String withdrawal=path(lead)+"/"+quote+"/withdraw";
  assertEquals(400,seller.send("POST",withdrawal,"{\"reason\":\"short\"}",true).statusCode());String longReason="Customer requested a new specification before agreement. ".repeat(20).substring(0,1000);assertEquals(200,seller.send("POST",withdrawal,json.writeValueAsString(Map.of("reason",longReason)),true).statusCode());assertEquals(longReason,jdbc.queryForObject("select withdrawal_reason from quote_versions where id=?",String.class,UUID.fromString(quote.toString())));assertEquals(409,buyer.send("POST",decision(lead,quote),"{\"action\":\"ACCEPT\",\"note\":\"\"}",true).statusCode());assertEquals("REVIEWING",jdbc.queryForObject("select status from enquiries where id=?",String.class,lead));
  var current=seller.send("POST",path(lead),issue(1,20000),true);Object accepted=data(current).get("id");assertEquals(200,buyer.send("POST",decision(lead,accepted),"{\"action\":\"ACCEPT\",\"note\":\"\"}",true).statusCode());Client parallel=login(sales);String orders="/api/v1/staff/enquiries/"+lead+"/orders",body=json.writeValueAsString(Map.of("quoteId",accepted));
  try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->seller.send("POST",orders,body,true));var b=pool.submit(()->parallel.send("POST",orders,body,true));assertEquals(200,a.get().statusCode());assertEquals(200,b.get().statusCode());}assertEquals(1,jdbc.queryForObject("select count(*) from commercial_orders where enquiry_id=?",Integer.class,lead));
  UUID legacyLead=lead(),legacyQuote=UUID.randomUUID();String longScope="Legacy specification details ".repeat(70);jdbc.update("insert into quote_versions(id,enquiry_id,version,scope,total_paise,terms,proposed_date,specification_snapshot,issued_by,state) values(?,?,1,?,1000,'Legacy terms',current_date+10,'{}'::jsonb,?,'ACCEPTED')",legacyQuote,legacyLead,longScope,sales);assertEquals(200,seller.send("POST","/api/v1/staff/enquiries/"+legacyLead+"/orders",json.writeValueAsString(Map.of("quoteId",legacyQuote)),true).statusCode());assertEquals(longScope,jdbc.queryForObject("select scope from commercial_orders where quote_id=?",String.class,legacyQuote));
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

}
