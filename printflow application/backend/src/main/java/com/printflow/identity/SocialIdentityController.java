package com.printflow.identity;
import jakarta.servlet.http.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.*;
import org.springframework.http.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
@RestController
public class SocialIdentityController {
 private final OidcProviders providers;private final SocialAccounts accounts;private final JdbcTemplate jdbc;
 public SocialIdentityController(OidcProviders providers,SocialAccounts accounts,JdbcTemplate jdbc){this.providers=providers;this.accounts=accounts;this.jdbc=jdbc;}
 public record Confirmation(@NotBlank @Size(max=72) String password){}
 @GetMapping("/api/public/sso/providers") public ResponseEntity<List<Map<String,Object>>> providers(){return response(java.util.stream.Stream.of("google","microsoft").map(id->Map.<String,Object>of("id",id,"name",id.equals("google")?"Google":"Microsoft","enabled",providers.enabled(id),"url","/api/public/sso/authorize/"+id)).toList());}
 @GetMapping("/api/public/sso/pending") public ResponseEntity<Map<String,Object>> pending(HttpSession session){var p=accounts.pending(session);return response(Map.of("provider",p.provider(),"email",p.email(),"name",p.name(),"emailVerified",p.trustedEmail()));}
 @PostMapping("/api/public/sso/complete") public ResponseEntity<Map<String,String>> complete(@Valid @RequestBody SocialAccounts.Signup input,HttpServletRequest request,HttpServletResponse response){var result=accounts.complete(input,request.getSession(),request.getRemoteAddr());if(!result.verificationRequired())accounts.signIn(result.user(),request,response);return response(Map.of("code",result.verificationRequired()?"CHECK_EMAIL_IF_ELIGIBLE":"SOCIAL_SIGNED_IN"));}
 @PostMapping("/api/v1/customer/sso/link/{provider}") public ResponseEntity<Map<String,String>> link(@AuthenticationPrincipal StaffPrincipal user,@PathVariable String provider,@Valid @RequestBody Confirmation input,HttpSession session){if(!providers.enabled(provider))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Provider not configured");accounts.prepareLink(user,provider,input.password(),session);return response(Map.of("url","/api/public/sso/authorize/"+provider));}
 @GetMapping("/api/v1/customer/sso") public ResponseEntity<List<String>> linked(@AuthenticationPrincipal StaffPrincipal user){return response(jdbc.query("select provider from external_identities where user_id=? order by provider",(rs,n)->rs.getString(1),user.id()));}
 private <T> ResponseEntity<T> response(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
