package org.tss.tm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.repository.SystemAdminRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.tenant.TenantContext;
import org.tss.tm.repository.TenantRepo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SystemAdminRepo systemAdminRepo;
    private final TenantUserRepo tenantUserRepo;
    private final TenantRepo tenantRepo;

    @Override
    @Transactional(readOnly = true)
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
        String tenantCode = tenantRepo.findTenantCodeBySchemaName(tenant)
                .orElseThrow(() -> new UsernameNotFoundException("Tenant not found"));

        return new CustomUserDetails(
                tenantUser.getEmail(),
                tenantUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + tenantUser.getRole().name())),
                tenantCode,
                false,
                tenantUser.getIsActive(),
                isAccountNonLocked(tenantUser.getLockedUntil()));
    }

    private boolean isAccountNonLocked(LocalDateTime lockedUntil) {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

}
