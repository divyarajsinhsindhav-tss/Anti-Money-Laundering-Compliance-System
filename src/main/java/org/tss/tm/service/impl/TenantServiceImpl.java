package org.tss.tm.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.common.enums.UserRole;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.dto.tenant.response.*;
import org.tss.tm.dto.tenant.response.RuleJobResponse;
import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.Customer;
import org.tss.tm.entity.tenant.CustomerError;
import org.tss.tm.entity.tenant.TransactionError;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.mapper.AlertMapper;
import org.tss.tm.mapper.CaseMapper;
import org.tss.tm.mapper.ScenarioMapper;
import org.tss.tm.mapper.TenantMapper;
import org.tss.tm.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;

import org.tss.tm.repository.*;
import org.tss.tm.service.interfaces.EmailService;
import org.tss.tm.service.interfaces.FlywayMigration;
import org.tss.tm.service.interfaces.TenantService;
import org.tss.tm.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.springframework.data.domain.Sort.Direction.DESC;

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
    private final CustomerRepo customerRepo;
    private final TransactionErrorRepo transactionErrorRepo;
    private final FinancialTransactionRepo financialTransactionRepo;
    private final JobRepo jobRepo;
    private final AlertRepo alertRepo;
    private final CaseRepo caseRepo;
    private final AlertMapper alertMapper;
    private final CaseMapper caseMapper;


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
                                  TenantAdminRegistrationRequest adminRequest) {
        Map<String, Object> variables = new java.util.HashMap<>();
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
                                          Tenant tenant) {
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
            throw new BusinessRuleException("tenantName contains invalid characters",
                    "INVALID_TENANT_NAME");
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

    @Override
    @Transactional(readOnly = true)
    public TransactionDashboardResponse getTransactionDashboardStats() {
        log.info("Fetching transaction dashboard stats for current tenant");

        Tenant tenant = getCurrentTenant();

        long totalTransactions = financialTransactionRepo.count();
        long totalErrors = transactionErrorRepo.count();
        long totalBatches = jobRepo.countByTenantAndJobType(tenant, JobType.FILE_UPLOAD_TRANSACTION);

        List<TransactionDashboardResponse.RecentErrorResponse> recentErrors = transactionErrorRepo.findAll(
                        PageRequest.of(0, 5, Sort.by(DESC, "createdAt")))
                .getContent().stream()
                .map(error -> TransactionDashboardResponse.RecentErrorResponse.builder()
                        .id(error.getErrorId())
                        .transactionId(error.getTxnNo())
                        .errorMessage(error.getCriticalErrors() != null
                                && !error.getCriticalErrors().isEmpty()
                                ? error.getCriticalErrors().get(0)
                                : "Data Validation Failure")
                        .errorCode("ERR_" + error.getErrorId())
                        .timestamp(error.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<TransactionDashboardResponse.RecentJobResponse> recentJobs = jobRepo.findByTenantAndJobType(
                        tenant, JobType.FILE_UPLOAD_TRANSACTION,
                        PageRequest.of(0, 5, Sort.by(DESC, "createdAt"))).getContent().stream()
                .map(job -> TransactionDashboardResponse.RecentJobResponse.builder()
                        .id(job.getJobId().toString())
                        .status(job.getStatus().name())
                        .errorCount(0)
                        .startTime(job.getStartedAt() != null ? job.getStartedAt()
                                : job.getCreatedAt())
                        .endTime(job.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        return TransactionDashboardResponse.builder()
                .totalTransactions(totalTransactions)
                .totalErrors(totalErrors)
                .totalBatches(totalBatches)
                .recentErrors(recentErrors)
                .recentJobs(recentJobs)
                .build();
    }

    @Override
    public List<TransactionDashboardResponse.RecentJobResponse> getTransactionJobs() {
        Tenant tenant = getCurrentTenant();
        return jobRepo.findByTenantAndJobType(
                        tenant, JobType.FILE_UPLOAD_TRANSACTION,
                        PageRequest.of(0, 10, Sort.by(DESC, "createdAt"))).getContent().stream()
                .map(job -> TransactionDashboardResponse.RecentJobResponse.builder()
                        .id(job.getJobId().toString())
                        .status(job.getStatus().name())
                        .errorCount(0)
                        .startTime(job.getStartedAt() != null ? job.getStartedAt()
                                : job.getCreatedAt())
                        .endTime(job.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileErrorResponse> getTransactionErrors(String jobId, Pageable pageable) {
        Page<TransactionError> errorsPage;

        if (jobId != null && !jobId.trim().isEmpty()) {
            errorsPage = transactionErrorRepo.findByJobId(jobId, pageable);
        } else {
            errorsPage = transactionErrorRepo.findAll(pageable);
        }

        return errorsPage.map(error -> FileErrorResponse.builder()
                .errorId(error.getErrorId())
                .jobId(error.getJobId())
                .identifier(error.getTxnNo())
                .rawRow(error.getRawRow())
                .criticalErrors(error.getCriticalErrors())
                .warningErrors(error.getWarningErrors())
                .createdAt(error.getCreatedAt())
                .sourceType("TRANSACTION")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardResponse getCustomerDashboardStats() {
        log.info("Fetching customer dashboard stats for current tenant");
        Tenant tenant = getCurrentTenant();

        long totalCustomers = customerRepo.count();
        long totalErrors = customerErrorRepo.count();
        long totalBatches = jobRepo.countByTenantAndJobType(tenant, JobType.FILE_UPLOAD_CUSTOMER);

        List<CustomerDashboardResponse.RecentErrorResponse> recentErrors = customerErrorRepo.findAll(
                        PageRequest.of(0, 5, Sort.by(DESC, "createdAt")))
                .getContent().stream()
                .map(error -> CustomerDashboardResponse.RecentErrorResponse.builder()
                        .id(error.getErrorId())
                        .cif(error.getCif())
                        .errorMessage(error.getCriticalErrors() != null
                                && !error.getCriticalErrors().isEmpty()
                                ? error.getCriticalErrors().get(0)
                                : "Data Validation Failure")
                        .errorCode("ERR_" + error.getErrorId())
                        .timestamp(error.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<CustomerDashboardResponse.RecentJobResponse> recentJobs = jobRepo.findByTenantAndJobType(
                        tenant, JobType.FILE_UPLOAD_CUSTOMER, PageRequest.of(0, 5, Sort.by(DESC, "createdAt")))
                .getContent().stream()
                .map(job -> CustomerDashboardResponse.RecentJobResponse.builder()
                        .id(job.getJobId().toString())
                        .status(job.getStatus().name())
                        .errorCount(0)
                        .startTime(job.getStartedAt() != null ? job.getStartedAt()
                                : job.getCreatedAt())
                        .endTime(job.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        return CustomerDashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalErrors(totalErrors)
                .totalBatches(totalBatches)
                .recentErrors(recentErrors)
                .recentJobs(recentJobs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDashboardResponse.RecentJobResponse> getCustomerJobs() {
        Tenant tenant = getCurrentTenant();
        return jobRepo.findByTenantAndJobType(
                        tenant, JobType.FILE_UPLOAD_CUSTOMER, PageRequest.of(0, 10, Sort.by(DESC, "createdAt")))
                .getContent().stream()
                .map(job -> CustomerDashboardResponse.RecentJobResponse.builder()
                        .id(job.getJobId().toString())
                        .status(job.getStatus().name())
                        .errorCount(0)
                        .startTime(job.getStartedAt() != null ? job.getStartedAt()
                                : job.getCreatedAt())
                        .endTime(job.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileErrorResponse> getCustomerErrors(String jobId, Pageable pageable) {
        Page<CustomerError> errorsPage;

        if (jobId != null && !jobId.trim().isEmpty()) {
            errorsPage = customerErrorRepo.findByJobId(jobId, pageable);
        } else {
            errorsPage = customerErrorRepo.findAll(pageable);
        }

        return errorsPage.map(error -> FileErrorResponse.builder()
                .errorId(error.getErrorId())
                .jobId(error.getJobId())
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
    public java.util.Map<String, Long> getRuleEngineStats() {
        Tenant tenant = getCurrentTenant();
        long totalScenarios = tenantScenarioRepo.countByTenant(tenant);
        long totalRuns = jobRepo.countByTenantAndJobType(tenant, JobType.RULE_ENGINE);

        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("totalScenarios", totalScenarios);
        stats.put("totalRuns", totalRuns);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RuleJobResponse.RecentJobResponse> getRuleEngineJobs(Pageable pageable) {
        Tenant tenant = getCurrentTenant();
        return jobRepo.findByTenantAndJobType(tenant, JobType.RULE_ENGINE, pageable)
                .map(job -> RuleJobResponse.RecentJobResponse.builder()
                        .id(job.getJobId().toString())
                        .status(job.getStatus().name())
                        .jobType(job.getJobType().name())
                        .startTime(job.getStartedAt() != null ? job.getStartedAt()
                                : job.getCreatedAt())
                        .endTime(job.getCompletedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerListResponse> getAllCustomers(String search, Pageable pageable) {
        Page<org.tss.tm.entity.tenant.Customer> customers;
        if (search != null && !search.isEmpty()) {
            customers = customerRepo.searchCustomers(search, pageable);
        } else {
            customers = customerRepo.findAll(pageable);
        }

        return customers.map(c -> CustomerListResponse.builder()
                .customerId(c.getCustomerId())
                .cif(c.getCif())
                .fullName(c.getFirstName() + " " + (c.getMiddleName() != null ? c.getMiddleName() + " " : "") + c.getLastName())
                .dob(c.getDob())
                .income(c.getIncome())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(java.util.UUID customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        return CustomerDetailResponse.builder()
                .customerId(customer.getCustomerId())
                .cif(customer.getCif())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .dob(customer.getDob())
                .income(customer.getIncome())
                .accounts(customer.getAccounts().stream()
                        .map(a -> CustomerDetailResponse.AccountResponse.builder()
                                .accountNumber(a.getAccountNumber())
                                .accountType(a.getAccountType())
                                .openedAt(a.getOpenedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionListResponse> getAllTransactions(String search, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<org.tss.tm.entity.tenant.FinancialTransaction> transactions;
        if ((search != null && !search.isEmpty()) || startDate != null || endDate != null) {
            transactions = financialTransactionRepo.searchTransactions(search != null ? search : "", startDate, endDate, pageable);
        } else {
            transactions = financialTransactionRepo.findAll(pageable);
        }

        return transactions.map(t -> TransactionListResponse.builder()
                .transactionId(t.getTransactionId())
                .txnNo(t.getTxnNo())
                .accountNumber(t.getAccount().getAccountNumber())
                .customerName(t.getAccount().getCustomer().getFirstName() + " " + t.getAccount().getCustomer().getLastName())
                .amount(t.getAmount())
                .txnType(t.getTxnType())
                .direction(t.getDirection())
                .txnTimestamp(t.getTxnTimestamp())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetail(UUID transactionId) {
        org.tss.tm.entity.tenant.FinancialTransaction t = financialTransactionRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));

        return TransactionDetailResponse.builder()
                .transactionId(t.getTransactionId())
                .txnNo(t.getTxnNo())
                .amount(t.getAmount())
                .txnType(t.getTxnType())
                .direction(t.getDirection())
                .counterpartyAccountNo(t.getCounterpartyAccountNo())
                .counterpartyBankIfsc(t.getCounterpartyBankIfsc())
                .txnTimestamp(t.getTxnTimestamp())
                .countryCode(t.getCountryCode())
                .account(TransactionDetailResponse.AccountInfo.builder()
                        .accountId(t.getAccount().getAccountId())
                        .accountNumber(t.getAccount().getAccountNumber())
                        .accountType(t.getAccount().getAccountType())
                        .build())
                .customer(TransactionDetailResponse.CustomerInfo.builder()
                        .customerId(t.getAccount().getCustomer().getCustomerId())
                        .cif(t.getAccount().getCustomer().getCif())
                        .fullName(t.getAccount().getCustomer().getFirstName() + " " + t.getAccount().getCustomer().getLastName())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDashboardResponse getDashboardStats() {
        log.info("Fetching unified dashboard stats for current tenant");

        long totalCustomers = customerRepo.count();
        long totalTransactions = financialTransactionRepo.count();
        long totalOpenAlerts = alertRepo.countByAlertStatus(AlertStatus.OPEN);
        long totalUnderReviewCases = caseRepo.countByStatus(CaseStatus.UNDER_REVIEW);

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        TenantUser user = tenantUserRepo.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("TenantUser", currentUserEmail));

        List<AlertResponse> recentAlerts = null;
        List<CaseResponse> recentCases = null;

        if (user.getRole() == UserRole.BANK_ADMIN) {
            recentAlerts = alertRepo.findAllByAlertStatus(AlertStatus.OPEN, PageRequest.of(0, 5, Sort.by(DESC, "createdAt")))
                    .getContent().stream()
                    .map(alertMapper::toResponse)
                    .collect(Collectors.toList());
        } else if (user.getRole() == UserRole.COMPLIANCE_OFFICER) {
            recentCases = caseRepo.findAllByStatus(CaseStatus.UNDER_REVIEW, PageRequest.of(0, 5, Sort.by(DESC, "createdAt")))
                    .getContent().stream()
                    .map(caseMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return TenantDashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalTransactions(totalTransactions)
                .totalOpenAlerts(totalOpenAlerts)
                .totalUnderReviewCases(totalUnderReviewCases)
                .recentAlerts(recentAlerts)
                .recentCases(recentCases)
                .build();
    }
}
