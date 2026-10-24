package com.printflow.enquiry;

import com.printflow.enquiry.StaffEnquiryService.*;
import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/enquiries")
public class StaffEnquiryController {
    private final StaffEnquiryService service;
    public StaffEnquiryController(StaffEnquiryService service){this.service=service;}
    @GetMapping public ResponseEntity<List<LeadView>> list(@AuthenticationPrincipal StaffPrincipal staff,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="ALL") String filter,@RequestParam(required=false) UUID before,@RequestParam(defaultValue="ALL") String status){return response(service.list(staff,search,filter,before,status));}
    @PostMapping public ResponseEntity<CreatedLead> create(@AuthenticationPrincipal StaffPrincipal staff,@Valid @RequestBody AssistedEnquiry input){
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.create(staff,input));
    }
    @PatchMapping("/{id}/assignment") public ResponseEntity<LeadView> assign(@AuthenticationPrincipal StaffPrincipal staff,@PathVariable UUID id,@Valid @RequestBody Assignment input){return response(service.assign(staff,id,input));}
    @PatchMapping("/{id}/follow-up") public ResponseEntity<LeadView> followUp(@AuthenticationPrincipal StaffPrincipal staff,@PathVariable UUID id,@Valid @RequestBody FollowUp input){return response(service.followUp(staff,id,input));}
    @PatchMapping("/{id}/status") public ResponseEntity<LeadView> status(@AuthenticationPrincipal StaffPrincipal staff,@PathVariable UUID id,@Valid @RequestBody StatusChange input){return response(service.status(staff,id,input));}
    @GetMapping("/{id}/activities") public ResponseEntity<List<Activity>> history(@AuthenticationPrincipal StaffPrincipal staff,@PathVariable UUID id){return response(service.history(staff,id));}
    private <T> ResponseEntity<T> response(T data){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);}
}
