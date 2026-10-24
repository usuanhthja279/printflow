package com.printflow.enquiry;

import com.printflow.enquiry.EnquiryDtos.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/enquiries")
public class PublicEnquiryController {
    private final EnquiryService service;
    private final com.printflow.shared.GuestChallenge challenge;
    private final boolean enabled;
    private final com.printflow.shared.RequestLimits limits;
    private final boolean privacyApproved;
    private final org.springframework.core.env.Environment environment;
    public PublicEnquiryController(EnquiryService service,
        @Value("${app.public-enquiries-enabled:false}") boolean enabled,com.printflow.shared.RequestLimits limits,@Value("${APP_PRIVACY_APPROVED:false}") boolean privacyApproved,org.springframework.core.env.Environment environment,com.printflow.shared.GuestChallenge challenge) {
        this.service=service;this.enabled=enabled;this.limits=limits;this.privacyApproved=privacyApproved;this.environment=environment;this.challenge=challenge;
    }
    @GetMapping("/policy") public java.util.Map<String,Object> policy(){return java.util.Map.of("privacyVersion",com.printflow.identity.CustomerIdentityController.PRIVACY,"challengeRequired",challenge.required(),"siteKey",challenge.siteKey());}
    @PostMapping
    public ResponseEntity<CreatedEnquiry> create(@Valid @RequestBody CreateEnquiry request,jakarta.servlet.http.HttpServletRequest http) {
        if (!enabled||(environment.matchesProfiles("prod")&&!privacyApproved)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Public enquiry intake disabled until production safeguards are implemented");
        limits.require("guest-ip",http.getRemoteAddr(),15);limits.require("guest-email",request.email().trim().toLowerCase(java.util.Locale.ROOT),5);
        if(!Boolean.TRUE.equals(request.privacyConsent())||!com.printflow.identity.CustomerIdentityController.PRIVACY.equals(request.privacyVersion())||request.website()!=null&&!request.website().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Review the privacy notice and consent before submitting");
        challenge.verify(request.challengeToken());
        return ResponseEntity.status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noStore()).body(service.create(request));
    }
    @GetMapping("/{publicId}")
    public ResponseEntity<TrackingView> track(@PathVariable UUID publicId,
        @RequestHeader(value="X-Tracking-Token",required=false) String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.track(publicId, token));
    }
}
