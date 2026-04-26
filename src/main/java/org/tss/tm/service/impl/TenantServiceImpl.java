package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.mapper.ScenarioMapper;
import org.tss.tm.repository.TenantScenarioRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tss.tm.dto.tenant.response.TenantAvailableResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
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
import org.tss.tm.service.interfaces.EmailService;
import org.tss.tm.tenant.TenantContext;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.tenant.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final EmailService emailService;
    private final TenantScenarioRepo tenantScenarioRepo;
    private final ScenarioMapper scenarioMapper;

    @Override
    public TenantResponse createTenant(TenantRegistrationRequest request, String email) {
        String tenantCode = request.getTenantCode();
        validateTenantName(tenantCode);

        if (tenantRepo.findByName(request.getName()).isPresent()) {
            throw new BusinessRuleException("Tenant with name " + request.getName() + " already exists",
                    "TENANT_ALREADY_EXISTS");
        }

        String schemaName = TenantConstants.SCHEMA_PREFIX + tenantCode.toLowerCase();

        // 1. Create Schema (Non-transactional in many DBs, or needs separate
        // connection)
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (SQLException e) {
            log.error("Failed to create schema for tenant: {}", schemaName, e);
            throw new BusinessRuleException("Failed to initialize tenant environment: " + e.getMessage(),
                    "SCHEMA_CREATION_FAILED");
        }

        // 2. Save Tenant Record (Transactional)
        Tenant savedTenant = transactionTemplate.execute(status -> {
            SystemAdmin admin = systemAdminRepo.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("SystemAdmin", email));

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

                // 5. Send Welcome Email
                sendWelcomeEmail(savedTenant, request.getAdminRegistrationRequest());

            } finally {
                TenantContext.clear();
            }
        }

        return tenantMapper.toResponse(savedTenant);
    }

    private void sendWelcomeEmail(Tenant tenant, TenantAdminRegistrationRequest adminRequest) {
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("tenantName", tenant.getName());
        variables.put("adminEmail", adminRequest.getEmail());

        emailService.sendHtmlEmail(
                adminRequest.getEmail(),
                "Welcome to AML Compliance System - " + tenant.getName(),
                "tenant-registration",
                variables);
    }

    @Override
    public TenantAvailableResponse tenantAvailable(String tenantCode) {
        boolean isExist = tenantRepo.existsTenantByTenantCode(tenantCode);
        return TenantAvailableResponse.builder()
                .isAvailable(isExist)
                .build();
    }

    public TenantUser registerTenantAdmin(TenantAdminRegistrationRequest adminRequest, Tenant tenant) {
        TenantUser user = userMapper.toEntity(adminRequest);
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
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantName));
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
            throw new BusinessRuleException("tenantName is null", "INVALID_TENANT_NAME");
        }
        if (!tenantName.matches("[a-zA-Z0-9_]+")) {
            throw new BusinessRuleException("tenantName contains invalid characters", "INVALID_TENANT_NAME");
        }
    }

    @Override
    public Tenant getCurrentTenant() {
        log.info("Current tenant: {}", TenantContext.getCurrentTenant());
        return tenantRepo
                .findTenantBySchemaName(TenantContext.getCurrentTenant())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tenant with schema", TenantContext.getCurrentTenant()));
    }

    @Override
    public String getTenantCodeBySchemaName(String schemaName) {
        return tenantRepo.findTenantCodeBySchemaName(schemaName)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant with schema", schemaName));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScenarioResponse> getScenarios(Pageable pageable) {
        Tenant tenant = getCurrentTenant();
        return tenantScenarioRepo.findAllByTenant(tenant, pageable)
                .map(mapping -> scenarioMapper.toResponse(mapping.getScenario()));
    }
}
