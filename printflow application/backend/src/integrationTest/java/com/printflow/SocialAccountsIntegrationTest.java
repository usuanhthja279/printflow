package com.printflow;
import com.printflow.identity.*;import java.time.Instant;import java.util.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.mock.web.*;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.oauth2.core.oidc.OidcIdToken;import org.springframework.security.oauth2.core.oidc.user.*;import org.springframework.test.annotation.DirtiesContext;import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
/** Tests local mapping after framework validation, not live provider signatures/consent. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","app.staff.bootstrap.enabled=false","app.owner.bootstrap.enabled=false","APP_CUSTOMER_ACCOUNTS_ENABLED=true","APP_MAIL_ENABLED=false","APP_MAIL_ENCRYPTION_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","server.servlet.session.cookie.secure=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SocialAccountsIntegrationTest {
 @Autowired SocialAccounts accounts;@Autowired CustomerIdentityController identity;@Autowired CustomerMail mail;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder passwords;@Autowired StaffDirectory directory;
 String prefix="it-sso-"+UUID.randomUUID();
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @AfterAll void cleanup(){jdbc.update("update staff_users set enabled=false where username like ?",prefix+"%");jdbc.update("update mail_outbox set payload=null,state='EXPIRED' where recipient like ?",prefix+"%");}
 OidcUser claim(String issuer,String subject,String email,boolean verified){return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),new OidcIdToken("synthetic-not-a-real-token",Instant.now().minusSeconds(5),Instant.now().plusSeconds(300),Map.of("iss",issuer,"sub",subject,"aud",List.of("synthetic-client"),"email",email,"email_verified",verified,"name","Synthetic Provider Customer")));}
 SocialAccounts.Signup signup(String email){return new SocialAccounts.Signup(email,"Synthetic Provider Customer","9000000000",true,"2026-10-23");}
 MockHttpServletRequest request(){var r=new MockHttpServletRequest();r.setSession(new MockHttpSession());return r;}
 @Test void verifiedGoogleCreatesOnlyCustomerAndStableSubjectPreservesIdentity(){
  String email=prefix+"-google@example.test",subject=UUID.randomUUID().toString();var request=request();var response=new MockHttpServletResponse();
  assertEquals("/customer?mode=social",accounts.callback("google",claim("https://accounts.google.com",subject,email,true),request,response));
  var result=accounts.complete(signup(email),request.getSession(),prefix+"-google");assertFalse(result.verificationRequired());assertEquals("CUSTOMER",result.user().role());assertTrue(result.user().enabled());
  var login=request();assertEquals("/customer",accounts.callback("google",claim("https://accounts.google.com",subject,"different@example.test",true),login,new MockHttpServletResponse()));
  assertEquals(result.user().id(),((StaffPrincipal)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id());
  jdbc.update("update staff_users set enabled=false where id=?",result.user().id());assertEquals("/customer?provider_error=verify_or_contact",accounts.callback("google",claim("https://accounts.google.com",subject,email,true),request(),new MockHttpServletResponse()));assertNull(SecurityContextHolder.getContext().getAuthentication());
 }
 @Test void matchingOwnerEmailNeverAutoLinksOrPromotesProvider(){
  String email=prefix+"-owner@example.test";UUID id=UUID.randomUUID();jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'OWNER')",id,email,passwords.encode(UUID.randomUUID().toString()));
  var request=request();accounts.callback("google",claim("https://accounts.google.com",UUID.randomUUID().toString(),email,true),request,new MockHttpServletResponse());
  assertEquals(409,assertThrows(ResponseStatusException.class,()->accounts.complete(signup(email),request.getSession(),prefix+"-collision")).getStatusCode().value());assertEquals(0,jdbc.queryForObject("select count(*) from external_identities where user_id=?",Integer.class,id));assertEquals("OWNER",jdbc.queryForObject("select role from staff_users where id=?",String.class,id));
 }
 @Test void microsoftEmailRequiresSameBrowserVerificationAndCannotBypassThroughResend(){
  String email=prefix+"-microsoft@example.test",subject=UUID.randomUUID().toString(),issuer="https://login.microsoftonline.com/11111111-2222-4333-8444-555555555555/v2.0";var original=request();accounts.callback("microsoft",claim(issuer,subject,email,true),original,new MockHttpServletResponse());
  var result=accounts.complete(signup(email),original.getSession(),prefix+"-microsoft");assertTrue(result.verificationRequired());assertFalse(result.user().enabled());
  String encrypted=jdbc.queryForObject("select payload from mail_outbox where user_id=?",String.class,result.user().id());String token=mail.decryptForDelivery(encrypted).split("#token=",2)[1].split("\\n",2)[0].trim();var input=new CustomerIdentityController.Token(token);
  assertEquals(400,assertThrows(ResponseStatusException.class,()->identity.verify(input,request())).getStatusCode().value());
  identity.resend(new CustomerIdentityController.EmailInput(email),request());assertEquals(1,jdbc.queryForObject("select count(*) from mail_outbox where user_id=?",Integer.class,result.user().id()));
  assertEquals(200,identity.verify(input,original).getStatusCode().value());assertEquals("/customer",accounts.callback("microsoft",claim(issuer,subject,email,true),request(),new MockHttpServletResponse()));
  assertEquals(result.user().id(),((StaffPrincipal)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id());
 }
 @Test void providerLinkRequiresCustomerPasswordAndExistingAccountNeverMergesByEmail(){
  String email=prefix+"-link@example.test",password=UUID.randomUUID().toString();UUID id=UUID.randomUUID();jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'CUSTOMER')",id,email,passwords.encode(password));jdbc.update("insert into customer_profiles(user_id,name,phone,email_verified,privacy_version) values(?,'Synthetic','9000000000',true,'2026-10-23')",id);var user=directory.loadUserByUsername(email);var request=request();
  assertEquals(403,assertThrows(ResponseStatusException.class,()->accounts.prepareLink(user,"google","incorrect",request.getSession())).getStatusCode().value());accounts.prepareLink(user,"google",password,request.getSession());String subject=UUID.randomUUID().toString();assertEquals("/customer",accounts.callback("google",claim("https://accounts.google.com",subject,"another@example.test",true),request,new MockHttpServletResponse()));assertEquals(id,jdbc.queryForObject("select user_id from external_identities where subject=?",UUID.class,subject));
 }
}
