package com.printflow.enquiry;

import com.printflow.enquiry.EnquiryDtos.*;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!prod")
@ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
@RequestMapping("/api/demo/enquiries")
public class DemoEnquiryController {
    private final EnquiryService service;
    private final String demoKey;
    public DemoEnquiryController(EnquiryService service,@Value("${app.demo.key:}") String demoKey) {
        this.service=service;this.demoKey=demoKey;
    }
    @PostConstruct
    void validateKey() {
        if (demoKey.length()<16) throw new IllegalStateException("APP_DEMO_KEY must contain at least 16 characters");
    }
    private void requireKey(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                demoKey.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid demo key");
    }
    @GetMapping
    public ResponseEntity<List<DemoEnquiry>> list(@RequestHeader(value="X-Demo-Key",required=false) String key) {
        requireKey(key);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.demoList());
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<DemoEnquiry> update(@PathVariable UUID id,
        @RequestHeader(value="X-Demo-Key",required=false) String key,
        @Valid @RequestBody UpdateStatus request) {
        requireKey(key);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.demoUpdate(id,request.status()));
    }
}
