package com.printflow.identity;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class StaffSessionController {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public StaffSessionController(org.springframework.jdbc.core.JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record SessionView(UUID id,String username,String role,boolean mustChangePassword,boolean mfaEnrolled,boolean mfaRequired,boolean mfaRecent,boolean contactsAccess) {}
    @GetMapping("/csrf") public ResponseEntity<Map<String,String>> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(Map.of("token",csrf.getToken(),"headerName",csrf.getHeaderName()));
    }
    @GetMapping("/session") public ResponseEntity<SessionView> session(@AuthenticationPrincipal StaffPrincipal staff,jakarta.servlet.http.HttpSession session) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new SessionView(staff.id(),staff.username(),staff.role(),staff.mustChangePassword(),Boolean.TRUE.equals(jdbc.queryForObject("select totp_secret is not null from staff_users where id=?",Boolean.class,staff.id())),staff.role().equals("OWNER")&&!OwnerMfaController.verified(session),OwnerMfaController.recent(session),staff.role().equals("OWNER")||!staff.role().equals("CUSTOMER")&&Boolean.TRUE.equals(jdbc.queryForObject("select contacts_access from staff_users where id=?",Boolean.class,staff.id()))));
    }
}
