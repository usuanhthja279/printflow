package com.printflow.customer;
import com.printflow.identity.StaffPrincipal;import jakarta.validation.Valid;import jakarta.servlet.http.HttpServletRequest;import java.util.*;import org.springframework.http.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
@RestController public class RazorpayController {
 private final RazorpayPayments gateway;private final Payments payments;public RazorpayController(RazorpayPayments gateway,Payments payments){this.gateway=gateway;this.payments=payments;}
 @GetMapping("/api/v1/design/enquiries/{id}/payments/gateway") public ResponseEntity<?> status(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id){payments.view(u,id);return ok(gateway.status());}
 @PostMapping("/api/v1/design/enquiries/{id}/payments/gateway/orders") public ResponseEntity<?> create(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody RazorpayPayments.Create input){return ok(gateway.create(u,id,input));}
 @PostMapping("/api/v1/design/enquiries/{id}/payments/gateway/verify") public ResponseEntity<?> verify(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody RazorpayPayments.Verify input){return ok(gateway.verify(u,id,input));}
 @PostMapping("/api/public/payments/razorpay/webhook") public ResponseEntity<?> webhook(HttpServletRequest request,@RequestHeader(value="X-Razorpay-Signature",required=false) String signature)throws java.io.IOException{byte[] body=request.getInputStream().readNBytes(65537);if(body.length>65536)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);gateway.webhook(body,signature);return ok(Map.of("code","RECEIVED"));}
 private ResponseEntity<?> ok(Object value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
}
