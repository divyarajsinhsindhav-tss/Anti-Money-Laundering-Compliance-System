package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.dto.tenant.response.ScenarioResponse;
import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.FileErrorResponse;
import org.tss.tm.dto.tenant.response.TenantAvailableResponse;
import org.tss.tm.dto.tenant.response.TenantDetailResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.exception.TenantMismatchException;
import org.tss.tm.mapper.ScenarioMapper;
import org.tss.tm.mapper.TenantMapper;
import org.tss.tm.mapper.UserMapper;
import org.tss.tm.repository.*;
import org.tss.tm.service.interfaces.EmailService;
import org.tss.tm.service.interfaces.FlywayMigration;
import org.tss.tm.service.interfaces.TenantService;
import org.tss.tm.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

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
    private final CustomerErrorRepo customerErrorRepo;
    private final TransactionErrorRepo transactionErrorRepo;
    private final JobRepo jobRepo;

    @Override
    public TenantResponse createTenant(TenantRegistrationRequest request, String email) {
        String tenantCode = request.getTenantCode();
        validateTenantName(tenantCode);

        if (tenantRepo.findByName(request.getName()).isPresent()) {
            throw new BusinessRuleException("Tenant with name " + request.getName() + " already exists",
                    "TENANT_ALREADY_EXISTS");
        }

        String schemaName = TenantConstants.SCHEMA_PREFIX + tenantCode.toLowerCase();

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (SQLException e) {
            log.error("Failed to create schema for tenant: {}", schemaName, e);
            throw new BusinessRuleException("Failed to initialize tenant environment: " + e.getMessage(),
                    "SCHEMA_CREATION_FAILED");
        }

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

        flywayMigration.migrateSchema(schemaName);

        if (request.getAdminRegistrationRequest() != null) {
            try {
                TenantContext.setCurrentTenant(schemaName);
                transactionTemplate.execute(status -> {
                    registerTenantAdmin(request.getAdminRegistrationRequest(), savedTenant);
                    return null;
                });

                sendWelcomeEmail(savedTenant, request.getAdminRegistrationRequest());

            } finally {
                TenantContext.clear();
            }
        }

        return tenantMapper.toResponse(savedTenant);
    }

    private void sendWelcomeEmail(Tenant tenant,
                                  TenantAdminRegistrationRequest adminRequest
    ) {
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

    public TenantUser registerTenantAdmin(TenantAdminRegistrationRequest adminRequest,
                                          Tenant tenant
    ) {
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
        String schemaName = TenantContext.getCurrentTenant();
        System.out.println("schemaName: " + schemaName);
        if (schemaName == null || schemaName.isEmpty()) {
            throw new ResourceNotFoundException("Tenant context is missing", "CONTEXT_MISSING");
        }
        log.info("Current tenant: {}", schemaName);
        return tenantRepo
                .findTenantBySchemaName(schemaName)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Tenant with schema", schemaName));
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
                .map(mapping -> scenarioMapper.toTenantResponse(mapping.getScenario()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileErrorResponse> getFileError(Pageable pageable) {
        // Ensure tenant context is valid
        getCurrentTenant();
        
        return customerErrorRepo.findAll(pageable).map(error -> FileErrorResponse.builder()
                .identifier(error.getCif())
                .rawRow(error.getRawRow())
                .criticalErrors(error.getCriticalErrors())
                .warningErrors(error.getWarningErrors())
                .createdAt(error.getCreatedAt())
                .sourceType("CUSTOMER")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetailResponse getTenantDetail(String tenantCode) {
        log.info("Fetching tenant details for code: {}", tenantCode);
        Tenant tenant = tenantRepo.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));

        List<ScenarioResponse> subscribedScenarios = tenantScenarioRepo.findAllByTenant(tenant)
                .stream()
                .map(mapping -> scenarioMapper.toTenantResponse(mapping.getScenario()))
                .toList();

        long jobRunCount = jobRepo.countByTenant(tenant);
        List<JobRecord> jobHistory = jobRepo.findByTenantOrderByCreatedAtDesc(tenant);

        return TenantDetailResponse.builder()
                .tenant(tenantMapper.toResponse(tenant))
                .subscribedScenarios(subscribedScenarios)
                .jobRunCount(jobRunCount)
                .jobHistory(tenantMapper.toJobResponseList(jobHistory))
                .build();
    }
}
