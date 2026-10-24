package com.printflow.identity;
import java.util.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController public class MailStatusController{private final JdbcTemplate db;private final CustomerMail mail;public MailStatusController(JdbcTemplate db,CustomerMail mail){this.db=db;this.mail=mail;}
 @GetMapping("/api/v1/owner/mail-status") public ResponseEntity<?> status(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("enabled",mail.deliveryEnabled(),"messages",db.queryForList("select id,kind,recipient,state,attempts,failure_code,created_at,sent_at from mail_outbox order by created_at desc,id desc limit 100")));}}
