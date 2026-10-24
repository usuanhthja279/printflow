package com.printflow.identity;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
@Profile("(dev | qa) & !prod")
@RequestMapping("/api/v1/owner/local-mail")
public class LocalMailController {
 private final JdbcTemplate jdbc;private final CustomerMail mail;
 public LocalMailController(JdbcTemplate jdbc,CustomerMail mail){this.jdbc=jdbc;this.mail=mail;}
 @GetMapping public ResponseEntity<List<Map<String,Object>>> local(){
  if(mail.deliveryEnabled())throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
  var rows=jdbc.queryForList("select id,recipient,kind,payload,created_at from mail_outbox where recipient like '%@example.test' and payload is not null and created_at>now()-interval '30 minutes' order by created_at desc limit 20");
  for(var row:rows){String encrypted=(String)row.remove("payload");try{row.put("body",mail.decryptForDelivery(encrypted));}catch(RuntimeException ignored){row.put("body","Unavailable: message belongs to a different environment key.");}}
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rows);
 }
}
