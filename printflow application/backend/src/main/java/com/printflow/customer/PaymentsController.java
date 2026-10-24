package com.printflow.customer;
import com.printflow.identity.StaffPrincipal;import jakarta.validation.Valid;import java.util.UUID;import org.springframework.http.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/design/enquiries/{id}/payments") public class PaymentsController {
 private final Payments payments;public PaymentsController(Payments payments){this.payments=payments;}
 @GetMapping public ResponseEntity<?> view(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id){return ok(payments.view(u,id));}
 @PostMapping("/policy") public ResponseEntity<?> policy(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Payments.Policy input){return ok(payments.configure(u,id,input));}
 @PostMapping("/receipts") public ResponseEntity<?> receipt(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Payments.Receipt input){return ok(payments.receive(u,id,input));}
 @PostMapping("/receipts/{receipt}/reverse") public ResponseEntity<?> reverse(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@PathVariable UUID receipt,@Valid @RequestBody Payments.Reverse input){return ok(payments.reverse(u,id,receipt,input));}
 @PostMapping("/invoices") public ResponseEntity<?> invoice(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody Payments.Issue input){return ok(payments.issue(u,id,input));}
 @GetMapping("/invoices/{invoice}/document") public ResponseEntity<byte[]> document(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@PathVariable UUID invoice){byte[] bytes=payments.document(u,id,invoice);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).header("Content-Disposition","inline; filename=J.D.Printex-Invoice.pdf").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox").contentLength(bytes.length).body(bytes);}
 private ResponseEntity<?> ok(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
