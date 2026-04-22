package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.repository.SystemAdminRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.service.interfaces.FlywayMigration;
import org.tss.tm.service.interfaces.TenantService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.mapper.TenantMapper;
import org.tss.tm.mapper.UserMapper;
import org.tss.tm.tenant.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final DataSource dataSource;
    private final FlywayMigration flywayMigration;
    private final TenantRepo tenantRepo;
    private final SystemAdminRepo systemAdminRepo;
    private final TenantUserRepo tenantUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;

    @Override
    public TenantResponse createTenant(TenantRegistrationRequest request, String email) throws SQLException {
        String tenantCode = request.getTenantCode();
        validateTenantName(tenantCode);

        if (tenantRepo.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Tenant with name " + request.getName() + " already exists");
        }

        String schemaName = TenantConstants.SCHEMA_PREFIX + tenantCode.toLowerCase();

        // 1. Create Schema (Non-transactional in many DBs, or needs separate connection)
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        }
        
        // 2. Save Tenant Record (Transactional)
        Tenant savedTenant = transactionTemplate.execute(status -> {
            SystemAdmin admin = systemAdminRepo.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in admin not found in system: " + email));

            Tenant tenant = tenantMapper.toEntity(request);
            tenant.setSchemaName(schemaName);
            tenant.setOnboardedByAdmin(admin);

            Tenant saved = tenantRepo.saveAndFlush(tenant);
            entityManager.refresh(saved);
            return saved;
        });

        // 3. Migrate Schema (Should be outside JPA transaction)
        flywayMigration.migrateSchema(schemaName);

        // 4. Register Admin User (New Transaction for Tenant Schema)
        if (request.getAdminRegistrationRequest() != null) {
            try {
                TenantContext.setCurrentTenant(schemaName);
                transactionTemplate.execute(status -> {
                    registerTenantAdmin(request.getAdminRegistrationRequest(), savedTenant);
                    return null;
                });
            } finally {
                TenantContext.clear();
            }
        }

        return tenantMapper.toResponse(savedTenant);
    }

    public TenantUser registerTenantAdmin(TenantAdminRegistrationRequest adminRequest, Tenant tenant) {
        TenantUser user = userMapper.toEntity(adminRequest);
        user.setTenant(tenant);
        user.setPasswordHash(passwordEncoder.encode(adminRequest.getPassword()));
        user.setRole(UserRole.BANK_ADMIN);
        user.setIsActive(true);

        TenantUser savedUser = tenantUserRepo.saveAndFlush(user);
        entityManager.refresh(savedUser);
        return savedUser;
    }

    @Override
    public Tenant getTenantByName(String tenantName) {
        return tenantRepo.findByName(tenantName)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantName));
    }

    @Override
    public List<TenantResponse> getAllTenants() {
        return tenantMapper.toResponseList(tenantRepo.findAll());
    }

    @Override
    public void migrateAllTenants() {
        tenantRepo.findAll().forEach(tenant -> {
            flywayMigration.migrateSchema(tenant.getSchemaName());
        });
    }

    private void validateTenantName(String tenantName) {
        if (tenantName == null) {
            throw new IllegalArgumentException("tenantName is null");
        }
        if (!tenantName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("tenantName contains invalid characters");
        }
    }

}
