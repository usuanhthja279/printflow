package com.printflow.shared;
import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.core.env.Environment;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.web.server.ResponseStatusException;import tools.jackson.databind.json.JsonMapper;
@Service
public class GuestChallenge {
 private final String secret,siteKey,hostname;private final boolean required;private final JsonMapper json;
 public GuestChallenge(Environment env,JsonMapper json,@Value("${APP_TURNSTILE_SECRET:}") String secret,@Value("${APP_TURNSTILE_SITE_KEY:}") String siteKey,@Value("${APP_TURNSTILE_HOSTNAME:jdprintex.in}") String hostname,@Value("${APP_TURNSTILE_ENABLED:false}") boolean enabled){this.secret=secret;this.siteKey=siteKey;this.hostname=hostname;this.required=enabled||env.matchesProfiles("prod");this.json=json;}
 public boolean required(){return required;}public String siteKey(){return siteKey;}public boolean configured(){return !secret.isBlank()&&!siteKey.isBlank();}
 public void verify(String token){
  if(!required)return;if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Guest verification is not configured");
  if(token==null||token.isBlank()||token.length()>2048)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete the guest verification challenge");
  try{
   String body="secret="+URLEncoder.encode(secret,StandardCharsets.UTF_8)+"&response="+URLEncoder.encode(token,StandardCharsets.UTF_8);
   var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(HttpRequest.newBuilder(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify")).timeout(Duration.ofSeconds(5)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200||response.body().length()>16384)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
   Map data=json.readValue(response.body(),Map.class);if(!Boolean.TRUE.equals(data.get("success"))||!hostname.equals(data.get("hostname"))||!"enquiry".equals(data.get("action")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Guest verification failed or expired. Try again");
  }catch(ResponseStatusException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Guest verification is temporarily unavailable");}
 }
}
