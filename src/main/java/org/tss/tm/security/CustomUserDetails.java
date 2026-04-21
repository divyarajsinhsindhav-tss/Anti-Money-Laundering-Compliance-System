package org.tss.tm.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.tenant.TenantUser;

import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {

    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private String tenant;
    private boolean isSystemAdmin;
    private boolean enabled;
    private boolean accountNonLocked;

    public CustomUserDetails(String username,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            String tenant,
            boolean isSystemAdmin,
            boolean enabled,
            boolean accountNonLocked) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.tenant = tenant;
        this.isSystemAdmin = isSystemAdmin;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
