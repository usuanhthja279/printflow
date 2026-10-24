package com.printflow.files;
import java.util.UUID;import java.io.IOException;import java.net.URLDecoder;import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.beans.factory.annotation.Value;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
@RestController public class IntakeArtworkController {
 private final DesignProofs proofs;private final com.printflow.shared.RequestLimits limits;private final boolean enabled,privacy;private final org.springframework.core.env.Environment env;
 public IntakeArtworkController(DesignProofs proofs,com.printflow.shared.RequestLimits limits,@Value("${app.public-enquiries-enabled:false}") boolean enabled,@Value("${APP_PRIVACY_APPROVED:false}") boolean privacy,org.springframework.core.env.Environment env){this.proofs=proofs;this.limits=limits;this.enabled=enabled;this.privacy=privacy;this.env=env;}
 @PostMapping("/api/public/enquiries/{id}/artwork") public ResponseEntity<?> upload(@PathVariable UUID id,@RequestHeader("X-Tracking-Token") String token,@RequestHeader("X-File-Name") String name,@RequestHeader("Content-Type") String mime,HttpServletRequest request)throws IOException{
  if(!enabled||(env.matchesProfiles("prod")&&!privacy))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
  limits.require("intake-upload-ip",request.getRemoteAddr(),20);if(name.length()>2160)throw new IllegalArgumentException("Filename too long");
  return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(proofs.guestUpload(id,token,URLDecoder.decode(name,StandardCharsets.UTF_8),mime,request.getInputStream()));
 }
}
