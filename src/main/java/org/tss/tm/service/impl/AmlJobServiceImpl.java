package org.tss.tm.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.repository.ScenarioRepo;
import org.tss.tm.ruleEngine.AmlExecutionEngine;
import org.tss.tm.ruleEngine.AmlScenarioBlueprint;
import org.tss.tm.ruleEngine.ScenarioFactory;
import org.tss.tm.service.interfaces.AmlJobService;
import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.service.interfaces.TenantService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlJobServiceImpl implements AmlJobService {

    private final ScenarioRepo scenarioRepo;
    private final ScenarioFactory scenarioFactory;
    private final AmlExecutionEngine executionEngine;
    private final TenantService tenantService;
    private final JobService jobService;

    @Override
    public void executeTenantScenarios(int adminDays) {

        UUID tenantId= tenantService.getCurrentTenant().getTenantId();
        UUID currentJobId =jobService.createNewJob(JobType.RULE_ENGINE).getJobId();

        List<Scenario> activeScenarios = scenarioRepo.findActiveScenariosByTenantId(tenantId);

        if (activeScenarios.isEmpty()) {
            log.warn("No active scenarios found for {}.", tenantId);
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Scenario dbScenario : activeScenarios) {
            String scenarioCode = dbScenario.getScenarioCode();
            UUID scenarioId = dbScenario.getScenarioId();

            try {
                AmlScenarioBlueprint blueprint = scenarioFactory.getBlueprint(scenarioCode, scenarioId);

                executionEngine.executeScenario(blueprint, adminDays, currentJobId);

                successCount++;
                log.info("Successfully processed Scenario: {}", scenarioCode);

            } catch (IllegalArgumentException e) {
                failureCount++;
                log.error("Configuration Error in Scenario {}: {}", scenarioCode, e.getMessage());
            } catch (Exception e) {
                failureCount++;
                log.error("Critical Failure executing Scenario {}. Skipping to next.", scenarioCode, e);
            }
        }

        log.info("AML Job Completed for Tenant {}. Success: {}, Failed: {}",
                tenantId, successCount, failureCount);
    }
}