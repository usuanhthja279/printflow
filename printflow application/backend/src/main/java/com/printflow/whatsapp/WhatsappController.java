package com.printflow.whatsapp;

import com.printflow.identity.StaffPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class WhatsappController {
    public record Reply(@NotBlank @Size(max=4000) String text) {}
    public record SimulatedInbound(@NotBlank @Size(max=32) String phone,@NotBlank @Size(max=4096) String text) {}
    private final WhatsappService service;
    private final Environment environment;
    public WhatsappController(WhatsappService service,Environment environment){this.service=service;this.environment=environment;}

    @GetMapping("/api/public/whatsapp/contact")
    public ResponseEntity<Map<String,Object>> contact(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.contact());}

    @GetMapping("/api/v1/staff/whatsapp")
    public ResponseEntity<?> list(@AuthenticationPrincipal StaffPrincipal actor){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor));}
    @GetMapping("/api/v1/staff/whatsapp/{id}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(actor,id));}
    @GetMapping("/api/v1/owner/whatsapp/simulator")
    public ResponseEntity<?> simulator(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("enabled",service.simulatorEnabled()));}
    @PostMapping("/api/v1/staff/whatsapp/{id}/claim")
    public ResponseEntity<?> claim(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.claim(actor,id));}
    @PostMapping("/api/v1/staff/whatsapp/{id}/resume")
    public ResponseEntity<?> resume(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id){service.resume(actor,id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status","resumed"));}
    @PostMapping("/api/v1/staff/whatsapp/{id}/reply")
    public ResponseEntity<?> reply(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable UUID id,@Valid @RequestBody Reply request){service.reply(actor,id,request.text());return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();}

    @PostMapping("/api/v1/owner/whatsapp/simulate")
    public ResponseEntity<?> simulate(@AuthenticationPrincipal StaffPrincipal actor,@Valid @RequestBody SimulatedInbound request){
        if(environment.matchesProfiles("prod")||!service.simulatorEnabled())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        if(actor==null||!"OWNER".equals(actor.role()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        service.receiveSimulated(request.phone(),request.text());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("status","accepted"));
    }
}
