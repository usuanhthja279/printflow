package com.printflow.quote;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class QuotationController {
 private final Quotations service;
 public QuotationController(Quotations service){this.service=service;}
 @GetMapping({"/api/v1/staff/enquiries/{lead}/quotes","/api/v1/customer/enquiries/{lead}/quotes"})
 public ResponseEntity<?> list(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead){return response(service.list(user,lead));}
 @PostMapping("/api/v1/staff/enquiries/{lead}/quotes")
 public ResponseEntity<?> issue(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead,@Valid @RequestBody Quotations.Issue input){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.issue(user,lead,input));}
 @PostMapping("/api/v1/customer/enquiries/{lead}/quotes/{quote}/decision")
 public ResponseEntity<?> decide(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead,@PathVariable UUID quote,@Valid @RequestBody Quotations.Decision input){return response(service.decide(user,lead,quote,input));}
 @GetMapping("/api/v1/staff/enquiries/{lead}/quote-draft") public ResponseEntity<?> draft(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead){return response(service.draft(user,lead));}
 @PutMapping("/api/v1/staff/enquiries/{lead}/quote-draft") public ResponseEntity<?> saveDraft(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead,@Valid @RequestBody Quotations.Draft input){return response(service.saveDraft(user,lead,input));}
 @PostMapping("/api/v1/staff/enquiries/{lead}/quotes/{quote}/withdraw") public ResponseEntity<?> withdraw(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead,@PathVariable UUID quote,@Valid @RequestBody Quotations.Reason input){return response(service.withdraw(user,lead,quote,input));}
 @GetMapping({"/api/v1/staff/enquiries/{lead}/quotes/{quote}/document","/api/v1/customer/enquiries/{lead}/quotes/{quote}/document"}) public ResponseEntity<byte[]> document(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead,@PathVariable UUID quote){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).header("Content-Disposition","attachment; filename=jdprintex-quotation-"+quote+".pdf").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").body(service.document(user,lead,quote));}
 @PostMapping("/api/v1/staff/enquiries/{lead}/artwork-request") public ResponseEntity<?> requestArtwork(@AuthenticationPrincipal StaffPrincipal user,@PathVariable UUID lead){return response(service.requestArtwork(user,lead));}
 private ResponseEntity<?> response(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
