package com.printflow;
import java.net.*;import java.net.http.*;
import org.junit.jupiter.api.Test;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;import org.springframework.test.annotation.DirtiesContext;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","server.servlet.session.cookie.secure=false","APP_GOOGLE_CLIENT_ID=synthetic-client","APP_GOOGLE_CLIENT_SECRET=synthetic-fixture-secret","APP_SESSION_SAME_SITE=lax","app.staff.bootstrap.enabled=false","app.owner.bootstrap.enabled=false","APP_MAIL_ENABLED=false"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OidcConfigurationIntegrationTest {
 @LocalServerPort int port;
 @Test void authorizationUsesStateNoncePkceAndRejectsUnsolicitedCallbackWithoutProviderCalls()throws Exception {
  var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String base="http://127.0.0.1:"+port;
  var start=client.send(HttpRequest.newBuilder(URI.create(base+"/api/public/sso/authorize/google")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,start.statusCode());String location=start.headers().firstValue("location").orElseThrow();assertTrue(location.startsWith("https://accounts.google.com/"));assertTrue(location.contains("code_challenge="));assertTrue(location.contains("code_challenge_method=S256"));assertTrue(location.contains("state="));assertTrue(location.contains("nonce="));assertFalse(location.contains("synthetic-fixture-secret"));
  var rejected=client.send(HttpRequest.newBuilder(URI.create(base+"/api/public/sso/callback/google?code=synthetic&state=invalid")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,rejected.statusCode());assertEquals("/customer?provider_error=signin_failed",rejected.headers().firstValue("location").orElseThrow());
  var session=client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/auth/session")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,session.statusCode());
 }
}
