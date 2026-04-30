package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.ErrorSeverity;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.RuleEngineError;
import org.tss.tm.repository.RuleEngineErrorRepo;
import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.service.interfaces.TenantService;
import org.tss.tm.tenant.TenantContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AmlExecutor {

    private final ScenarioFactory scenarioFactory;
    private final ScenarioExecutor scenarioExecutor;
    private final TenantService tenantService;
    private final JobService jobService;
    private final RuleEngineErrorRepo ruleEngineErrorRepo;

    @Async("Rule Engine Executor")
    public void executeAmlJob(UUID currentJobId, List<Scenario> activeScenarios, String schemaName, LocalDate fromDate, LocalDate toDate) {
        try {
            TenantContext.setCurrentTenant(schemaName);
            int successCount = 0;
            int failureCount = 0;

            String tenantCode = tenantService.getCurrentTenant().getTenantCode();
            jobService.updateJobStatus(currentJobId, JobStatus.RUNNING);

            for (Scenario dbScenario : activeScenarios) {
                String scenarioCode = dbScenario.getScenarioCode();
                UUID scenarioId = dbScenario.getScenarioId();

                try {
                    AmlScenarioBlueprint blueprint = scenarioFactory.getBlueprint(scenarioCode);
                    blueprint.setScenarioId(scenarioId);

                    scenarioExecutor.runScenario(blueprint, currentJobId, fromDate, toDate);

                    successCount++;
                    log.info("Successfully processed Scenario: {}", scenarioCode);

                } catch (IllegalArgumentException e) {
                    failureCount++;
                    RuleEngineError ruleEngineError=RuleEngineError.builder().info("Scenario Failure: Wrong Argument error").scenarioCode(dbScenario.getScenarioCode()).jobId(String.valueOf(currentJobId)).severity(ErrorSeverity.HIGH).build();
                    ruleEngineErrorRepo.save(ruleEngineError);
                    log.error("Configuration Error in Scenario {}: {}", scenarioCode, e.getMessage());
                } catch (Exception e) {
                    failureCount++;
                    RuleEngineError ruleEngineError=RuleEngineError.builder().info("Scenario Failure: Execution Failed").scenarioCode(dbScenario.getScenarioCode()).jobId(String.valueOf(currentJobId)).severity(ErrorSeverity.HIGH).build();
                    ruleEngineErrorRepo.save(ruleEngineError);
                    log.error("Critical Failure executing Scenario {}. Skipping to next.", scenarioCode, e);
                }
            }

            if (successCount == 0) {
                jobService.updateJobStatus(currentJobId, JobStatus.FAILED);
                RuleEngineError ruleEngineError=RuleEngineError.builder().info("Job Failure: All scenarios execution failed").jobId(String.valueOf(currentJobId)).severity(ErrorSeverity.HIGH).build();
                ruleEngineErrorRepo.save(ruleEngineError);
                log.warn("Job Execution Failed for Tenant: {}", tenantCode);
                return;
            }
            jobService.updateJobStatus(currentJobId, JobStatus.COMPLETED);
            log.info("AML Job Completed for Tenant {}. Success: {}, Failed: {}",
                    tenantCode, successCount, failureCount);
        } finally {
            TenantContext.clear();
        }
    }
}