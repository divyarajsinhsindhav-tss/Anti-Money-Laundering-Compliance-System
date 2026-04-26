package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final TenantRepo tenantRepo;
    private final TenantScenarioRepo tenantScenarioRepo;
    private final ScenarioRepo scenarioRepo;
    private final ScenarioMapper scenarioMapper;

    @Override
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
    }

    @Override
    public Page<ScenarioResponse> getScenarios(Pageable pageable) {
        return scenarioRepo.findAll(pageable)
                .map(scenarioMapper::toResponse);
    }

    @Override
    public ScenarioDetailResponse getScenario(String code) {
        Scenario scenario = scenarioRepo.findScenarioByScenarioCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("SCENARIO", code));
        return scenarioMapper.toDetailResponse(scenario);
    }
}
