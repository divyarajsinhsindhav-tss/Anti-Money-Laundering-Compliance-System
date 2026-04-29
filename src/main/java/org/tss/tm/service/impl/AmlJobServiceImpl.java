package org.tss.tm.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.dto.tenant.response.RuleEngineResponse;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.repository.ScenarioRepo;
import org.tss.tm.ruleEngine.AmlExecutor;
import org.tss.tm.ruleEngine.ScenarioExecutor;
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
    private final TenantService tenantService;
    private final JobService jobService;
    private final AmlExecutor amlExecutor;

    @Override
    public RuleEngineResponse executeTenantScenarios() {

        UUID tenantId= tenantService.getCurrentTenant().getTenantId();
        UUID currentJobId =jobService.createNewJob(JobType.RULE_ENGINE).getJobId();

        List<Scenario> activeScenarios = scenarioRepo.findActiveScenariosByTenantId(tenantId);

        if (activeScenarios.isEmpty()) {
            throw new ResourceNotFoundException("No Active Scenario Found.", new Scenario());
        }
        String tenantName = tenantService.getCurrentTenant().getSchemaName();
        amlExecutor.executeAmlJob(currentJobId, activeScenarios, tenantName);

        return RuleEngineResponse.builder().jobId(String.valueOf(currentJobId)).build();
    }
}