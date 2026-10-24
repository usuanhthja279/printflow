package com.printflow.identity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record StaffPrincipal(UUID id, String username, String password, String role, boolean enabled, boolean mustChangePassword, long authVersion)
        implements UserDetails {
    public StaffPrincipal(UUID id,String username,String password,String role,boolean enabled) { this(id,username,password,role,enabled,false,0); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+role));
    }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public String toString(){return "StaffPrincipal[id="+id+", role="+role+"]";}
}
