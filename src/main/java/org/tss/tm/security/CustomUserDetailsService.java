package org.tss.tm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.repository.SystemAdminRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.tenant.TenantContext;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SystemAdminRepo systemAdminRepo;
    private final TenantUserRepo tenantUserRepo;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenant = TenantContext.getCurrentTenant();

        if (tenant == null || tenant.equals(TenantConstants.DEFAULT_TENANT)) {
            SystemAdmin sysadmin = systemAdminRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return new CustomUserDetails(
                    sysadmin.getEmail(),
                    sysadmin.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")),
                    "public",
                    true,
                    sysadmin.getIsActive(),
                    true);
        }
        TenantUser tenantUser = tenantUserRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(
                tenantUser.getEmail(),
                tenantUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + tenantUser.getRole().name())),
                tenantUser.getTenant().getTenantCode(),
                false,
                tenantUser.getIsActive(),
                isAccountNonLocked(tenantUser.getLockedUntil()));
    }

    private boolean isAccountNonLocked(java.time.LocalDateTime lockedUntil) {
        return lockedUntil == null || lockedUntil.isBefore(java.time.LocalDateTime.now());
    }

}
