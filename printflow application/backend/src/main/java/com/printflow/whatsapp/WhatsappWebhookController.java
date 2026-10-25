package com.printflow.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/whatsapp/webhook")
public class WhatsappWebhookController {
    private static final int MAX_BYTES=262_144;
    private final WhatsappSettings settings;
    private final WhatsappService service;
    public WhatsappWebhookController(WhatsappSettings settings,WhatsappService service){this.settings=settings;this.service=service;}

    @GetMapping public ResponseEntity<String> verify(@RequestParam(name="hub.mode",required=false) String mode,@RequestParam(name="hub.verify_token",required=false) String token,@RequestParam(name="hub.challenge",required=false) String challenge){
        if(!settings.webhookConfigured()||!"subscribe".equals(mode)||token==null||challenge==null||challenge.length()>256||!constantEquals(settings.verifyToken(),token))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(challenge);
    }
    @PostMapping(consumes="application/json") public ResponseEntity<?> receive(@RequestBody byte[] raw,@RequestHeader(value="X-Hub-Signature-256",required=false) String signature,@RequestHeader(value="Content-Length",required=false) Long contentLength){
        if(contentLength!=null&&contentLength>MAX_BYTES||raw.length>MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        if(!settings.webhookConfigured()||!MetaWebhookSignature.valid(raw,signature,settings.appSecret()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        service.acceptWebhook(raw);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of("status","accepted"));
    }
    private static boolean constantEquals(String left,String right){return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));}
}
