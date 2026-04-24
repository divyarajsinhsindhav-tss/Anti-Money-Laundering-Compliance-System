package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
import org.tss.tm.dto.user.request.ComplianceOfficerRegistrationRequest;
import org.tss.tm.dto.user.response.UserResponse;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.mapper.UserMapper;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.service.interfaces.TenantUserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantUserServiceImpl implements TenantUserService {

    private final TenantUserRepo tenantUserRepo;
    private final TenantRepo tenantRepo;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerComplianceOfficer(ComplianceOfficerRegistrationRequest request, String currentTenantSchema) {
        log.info("Registering compliance officer for tenant schema: {}", currentTenantSchema);

        Tenant tenant = tenantRepo.findBySchemaName(currentTenantSchema)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for schema: " + currentTenantSchema));

        if (tenantUserRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists in this tenant");
        }

        TenantUser user = userMapper.toEntity(request);
        user.setTenant(tenant);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.COMPLIANCE_OFFICER);
        user.setIsActive(true);

        TenantUser savedUser = tenantUserRepo.saveAndFlush(user);
        entityManager.refresh(savedUser);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserResponse getTenantBasicDetails(String userEmail) {
        TenantUser user = tenantUserRepo.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        return userMapper.toTenantUserResponse(user);
    }
}
