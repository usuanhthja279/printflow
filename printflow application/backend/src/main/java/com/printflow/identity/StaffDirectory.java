package com.printflow.identity;

import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffDirectory implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public StaffDirectory(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public boolean current(StaffPrincipal staff) {
        return Integer.valueOf(1).equals(jdbc.queryForObject(
            "select count(*) from staff_users where id=? and enabled=true and role=? and username=? and password_hash=? and must_change_password=? and auth_version=? and not exists(select 1 from customer_profiles p where p.user_id=staff_users.id and p.temporary_password_expires_at<=now())",
            Integer.class,staff.id(),staff.role(),staff.username(),staff.password(),staff.mustChangePassword(),staff.authVersion()));
    }
    @Override public StaffPrincipal loadUserByUsername(String username) {
        return jdbc.query("select id,username,password_hash,role,(enabled and not exists(select 1 from customer_profiles p where p.user_id=staff_users.id and p.temporary_password_expires_at<=now())) as enabled,must_change_password,auth_version from staff_users where username=?",
            (rs,row)->new StaffPrincipal(rs.getObject("id",java.util.UUID.class),rs.getString("username"),
                rs.getString("password_hash"),rs.getString("role"),rs.getBoolean("enabled"),rs.getBoolean("must_change_password"),rs.getLong("auth_version")),
            username.trim().toLowerCase(Locale.ROOT)).stream().findFirst()
            .orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
    }
}
