package com.printflow.quote;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class CommercialOrderController {
 private final CommercialOrders orders;
 public CommercialOrderController(CommercialOrders orders){this.orders=orders;}
 @GetMapping({"/api/v1/staff/enquiries/{id}/orders","/api/v1/customer/enquiries/{id}/orders"}) public ResponseEntity<?> list(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.list(u,id));}
 @PostMapping("/api/v1/staff/enquiries/{id}/orders") public ResponseEntity<?> convert(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody CommercialOrders.Convert input){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.convert(u,id,input));}
 @PostMapping("/api/v1/staff/enquiries/{id}/order-artwork") public ResponseEntity<?> link(@AuthenticationPrincipal StaffPrincipal u,@PathVariable UUID id,@Valid @RequestBody CommercialOrders.Link input){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(orders.link(u,id,input));}
}
