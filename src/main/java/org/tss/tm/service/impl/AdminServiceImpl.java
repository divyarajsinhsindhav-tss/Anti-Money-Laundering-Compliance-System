package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.TenantStatus;
import org.tss.tm.dto.admin.request.AssignScenarioRequest;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.system.TenantScenarioMapping;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.mapper.ScenarioMapper;
import org.tss.tm.repository.ScenarioRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.repository.TenantScenarioRepo;
import org.tss.tm.service.interfaces.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.entity.system.ScenarioParameterMaster;
import org.tss.tm.repository.ScenarioParameterMasterRepo;
import org.tss.tm.service.interfaces.ParamService;
import org.tss.tm.tenant.TenantContext;

import java.util.List;
import jakarta.persistence.EntityManager;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.StatusBasic;
import org.tss.tm.dto.admin.response.DashboardStatsResponse;
import org.tss.tm.repository.JobRepo;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final TenantRepo tenantRepo;
    private final TenantScenarioRepo tenantScenarioRepo;
    private final ScenarioRepo scenarioRepo;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioParameterMasterRepo scenarioParameterMasterRepo;
    private final ParamService scenarioParamService;
    private final JobRepo jobRepo;
    private final EntityManager entityManager;
    private final ParamService paramService;

    @Override
    @Transactional
    public void assignScenario(AssignScenarioRequest assignScenarioRequest) {
        Tenant tenant = tenantRepo.findByTenantCode(assignScenarioRequest.getTenantCode())
                .orElseThrow(() -> new ResourceNotFoundException("TENANT", assignScenarioRequest.getTenantCode()));

        Scenario scenario = scenarioRepo.findScenarioByScenarioCode(assignScenarioRequest.getScenarioCode())
                .orElseThrow(() -> new ResourceNotFoundException("SCENARIO", assignScenarioRequest.getScenarioCode()));

        TenantScenarioMapping tenantScenarioMapping = TenantScenarioMapping.builder()
                .tenant(tenant)
                .scenario(scenario)
                .isEnabled(true)
                .build();

        tenantScenarioRepo.save(tenantScenarioMapping);

        if (tenantScenarioRepo.countByTenant(tenant) == 1 && tenant.getStatus() == TenantStatus.ONBOARDING) {
            log.info("Activating tenant {} as first scenario is assigned", tenant.getTenantCode());
            tenant.setStatus(TenantStatus.ACTIVE);
            tenantRepo.save(tenant);
        }

        List<ScenarioParameterMaster> masterParams = scenarioParameterMasterRepo
                .findByScenario_ScenarioId(scenario.getScenarioId());

        log.info("Found {} master parameters for scenario {}", masterParams.size(), scenario.getScenarioCode());

        if (!masterParams.isEmpty()) {
            String previousTenant = TenantContext.getCurrentTenant();
            try {
                TenantContext.setCurrentTenant(tenant.getSchemaName());
                paramService.createParametersFromMaster(scenario, masterParams);
            } finally {
                TenantContext.setCurrentTenant(previousTenant);
            }
        } else {
            log.warn("No master parameters found for scenario {}. Skipping parameter copy.",
                    scenario.getScenarioCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScenarioResponse> getScenarios(Pageable pageable) {
        return scenarioRepo.findAll(pageable)
                .map(scenarioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ScenarioDetailResponse getScenario(String code) {
        Scenario scenario = scenarioRepo.findScenarioByScenarioCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("SCENARIO", code));
        log.info("Found scenario {} with {} rules and {} tenant mappings",
                scenario.getScenarioCode(),
                scenario.getRules() != null ? scenario.getRules().size() : 0,
                scenario.getTenantScenarios() != null ? scenario.getTenantScenarios().size() : 0);
        return scenarioMapper.toDetailResponse(scenario);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        String dbSize = "N/A";
        try {
            dbSize = (String) entityManager
                    .createNativeQuery("SELECT pg_size_pretty(pg_database_size(current_database()))").getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to fetch database size: {}", e.getMessage());
        }

        List<DashboardStatsResponse.RecentJobResponse> recentJobs = jobRepo.findRecentJobs(PageRequest.of(0, 5))
                .stream()
                .map(job -> DashboardStatsResponse.RecentJobResponse.builder()
                        .jobId(job.getJobId())
                        .tenantName(job.getTenant().getName())
                        .tenantCode(job.getTenant().getTenantCode())
                        .jobType(job.getJobType())
                        .status(job.getStatus())
                        .createdAt(job.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalTenants(tenantRepo.count())
                .totalActiveScenarios(scenarioRepo.countByStatus(StatusBasic.ACTIVE))
                .totalDbStorage(dbSize)
                .totalJobs(jobRepo.count())
                .runningJobs(jobRepo.countByStatus(JobStatus.RUNNING))
                .pendingJobs(jobRepo.countByStatus(JobStatus.PENDING))
                .failedJobs(jobRepo.countByStatus(JobStatus.FAILED))
                .completedJobs(jobRepo.countByStatus(JobStatus.COMPLETED))
                .recentJobs(recentJobs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardStatsResponse.RecentJobResponse> getJobRecords(JobStatus status, String tenantCode,
            Pageable pageable) {
        return jobRepo.findAllJobs(status, tenantCode, pageable)
                .map(job -> DashboardStatsResponse.RecentJobResponse.builder()
                        .jobId(job.getJobId())
                        .tenantName(job.getTenant().getName())
                        .tenantCode(job.getTenant().getTenantCode())
                        .jobType(job.getJobType())
                        .status(job.getStatus())
                        .createdAt(job.getCreatedAt())
                        .build());
    }
}
