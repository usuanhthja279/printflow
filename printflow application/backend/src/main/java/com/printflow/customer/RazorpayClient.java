package com.printflow.customer;

import java.net.URI;import java.net.http.*;import java.time.Duration;import java.nio.charset.StandardCharsets;import java.util.*;
import org.springframework.core.env.Environment;import org.springframework.stereotype.Component;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/** Test keys only; fixed official origin, no secret logging, bounded requests. */
@Component public class RazorpayClient {
 private final String key,secret,webhookSecret;private final boolean production;private final JsonMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
 public RazorpayClient(Environment env,JsonMapper json){key=env.getProperty("RAZORPAY_KEY_ID","");secret=env.getProperty("RAZORPAY_KEY_SECRET","");webhookSecret=env.getProperty("RAZORPAY_WEBHOOK_SECRET","");production=env.matchesProfiles("prod");this.json=json;}
 public boolean configured(){return !production&&key.startsWith("rzp_test_")&&!secret.isBlank();}
 public boolean webhooks(){return configured()&&!webhookSecret.isBlank();}
 public String key(){return key;}
 public void enabled(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Razorpay test keys are not configured. Manual payments remain available.");}
 public Map<String,Object> order(long amount,String receipt){return request("POST","orders",Map.of("amount",amount,"currency","INR","receipt",receipt,"partial_payment",false));}
 public Map<String,Object> payment(String id){if(!id.matches("pay_[A-Za-z0-9]+"))throw new IllegalArgumentException("Invalid payment identifier");return request("GET","payments/"+id,null);}
 @SuppressWarnings("unchecked") private Map<String,Object> request(String method,String path,Object body){enabled();try{var req=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/"+path)).timeout(Duration.ofSeconds(4)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((key+":"+secret).getBytes(StandardCharsets.UTF_8))).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();var r=http.send(req,HttpResponse.BodyHandlers.ofInputStream());try(var in=r.body()){byte[] data=in.readNBytes(65537);if(r.statusCode()<200||r.statusCode()>299||data.length>65536)throw new IllegalStateException();return json.readValue(data,Map.class);}}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay did not confirm this request. Retry or ask the team to reconcile it.");}}
 public boolean checkoutSignature(String order,String payment,String signature){return signature(secret,(order+"|"+payment).getBytes(StandardCharsets.UTF_8),signature);}
 public boolean webhookSignature(byte[] body,String signature){return webhooks()&&signature(webhookSecret,body,signature);}
 public static boolean signature(String secret,byte[] body,String signature){try{if(signature==null||!signature.matches("[a-fA-F0-9]{64}"))return false;var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return java.security.MessageDigest.isEqual(mac.doFinal(body),HexFormat.of().parseHex(signature));}catch(Exception e){return false;}}
}
