package com.printflow.enquiry;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/owner/enquiries")
public class OwnerEnquiryController {
 private final StaffEnquiryService enquiries;
 public OwnerEnquiryController(StaffEnquiryService enquiries){this.enquiries=enquiries;}
 @PatchMapping("/{id}/assignment") public ResponseEntity<StaffEnquiryService.LeadView> reassign(@AuthenticationPrincipal StaffPrincipal owner,@PathVariable UUID id,@Valid @RequestBody StaffEnquiryService.OwnerAssignment input){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(enquiries.reassign(owner,id,input));}
}
