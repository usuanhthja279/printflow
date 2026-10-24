package com.printflow.identity;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("(dev | qa) & !prod")
@ConditionalOnProperty(name="app.staff.bootstrap.enabled",havingValue="true")
public class LocalStaffBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final String username, password;
    public LocalStaffBootstrap(JdbcTemplate jdbc, PasswordEncoder passwords,
        @Value("${app.staff.bootstrap.username:}") String username,
        @Value("${app.staff.bootstrap.password:}") String password) {
        this.jdbc=jdbc;this.passwords=passwords;this.username=username.trim().toLowerCase(Locale.ROOT);this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (!username.matches("[a-z0-9@._+\\-]{3,140}") || password.length()<14 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Local staff bootstrap requires a valid username and 14–72 byte password");
        jdbc.update("insert into staff_users(id,username,password_hash,role) values(?,?,?,'SALES') on conflict(username) do nothing",
            UUID.randomUUID(),username,passwords.encode(password));
    }
}
