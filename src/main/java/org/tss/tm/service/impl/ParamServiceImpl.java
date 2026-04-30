package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.dto.tenant.request.ScenarioParamUploadRequest;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.ScenarioParameterMaster;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.repository.RuleRepo;
import org.tss.tm.repository.ScenarioParamRepo;
import org.tss.tm.repository.ScenarioRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.service.interfaces.ScenarioParamService;
import org.tss.tm.tenant.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScenarioParamServiceImpl implements ScenarioParamService {

    private final ScenarioParamRepo scenarioParamRepo;
    private final ScenarioRepo scenarioRepo;
    private final RuleRepo ruleRepo;
    private final TenantRepo tenantRepo;

    @Override
    public Map<String, Map<String, Object>> getParams(UUID scenarioId) {
        if(!scenarioRepo.existsById(scenarioId)){
            throw new ResourceNotFoundException("Scenario Not Found", new Scenario());
        }

        List<ScenarioParam> activeParams = scenarioParamRepo.findActiveParametersForScenario(scenarioId);

        if(activeParams.isEmpty()){
            throw new ResourceNotFoundException("Parameters Not Found", new ScenarioParam());
        }

        Map<String, Map<String, Object>> params = new HashMap<>();

        for(ScenarioParam param : activeParams){
            String ruleCode;
            String paramKey;
            if(param.getRule()==null){
                ruleCode="COMMON";
                paramKey=param.getParamKey().toUpperCase();
            }else{
                ruleCode = param.getRule().getRuleCode();
                paramKey = param.getParamKey().toUpperCase();
            }

            Map<String, Object> ruleParams = params.computeIfAbsent(ruleCode, k -> new HashMap<>());

            switch (param.getDataType()){
                case INT -> ruleParams.put(paramKey, param.getIntValue());
                case DECIMAL -> ruleParams.put(paramKey, param.getDecimalValue());
                default -> ruleParams.put(paramKey, param.getStringValue());
            }
        }

        return params;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createParametersFromMaster(Scenario scenario, List<ScenarioParameterMaster> masterParams) {
        String schemaName = TenantContext.getCurrentTenant();
        log.info("Creating {} parameters for scenario {} in tenant schema {}", 
                masterParams.size(), scenario.getScenarioCode(), schemaName);
        
        Tenant tenant = tenantRepo.findTenantBySchemaName(schemaName)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant with schema", schemaName));

        for (ScenarioParameterMaster master : masterParams) {
            ScenarioParam tenantParam = ScenarioParam.builder()
                    .tenant(tenant)
                    .scenario(scenario)
                    .rule(master.getRule())
                    .paramKey(master.getParameterKey())
                    .dataType(master.getDataType())
                    .validFrom(LocalDateTime.now())
                    .build();

            if (master.getDefaultValue() != null && !master.getDefaultValue().isEmpty()) {
                try {
                    switch (master.getDataType()) {
                        case STRING -> tenantParam.setStringValue(master.getDefaultValue());
                        case INT -> tenantParam.setIntValue(Long.parseLong(master.getDefaultValue()));
                        case DECIMAL -> tenantParam.setDecimalValue(new BigDecimal(master.getDefaultValue()));
                        case BOOLEAN, DATE -> tenantParam.setStringValue(master.getDefaultValue());
                    }
                } catch (Exception e) {
                    log.error("Error parsing default value for parameter {}: {}", master.getParameterKey(), e.getMessage());
                    tenantParam.setStringValue(master.getDefaultValue());
                }
            }
            scenarioParamRepo.save(tenantParam);
        }
    }

    @Override
    public ScenarioParam convertToEntity(ScenarioParamUploadRequest request) {
        if(request.getParamKey()==null || request.getParamKey().isEmpty()){
            throw new IllegalArgumentException("Parameter Key Not Found");
        }

        if(request.getValue() ==null || request.getValue().isEmpty()){
            throw new IllegalArgumentException("Missing Value");
        }

        if(request.getScenarioCode()==null || request.getScenarioCode().isEmpty()){
            throw new IllegalArgumentException("Missing Scenario Code");
        }
        if(request.getDataType() ==null){
            throw new IllegalArgumentException("Missing Data Type");
        }

        Scenario scenario= scenarioRepo.findScenarioByScenarioCode(request.getRuleCode()).orElseThrow(()-> new ResourceNotFoundException("Scenario Not Found",new Scenario()));

        ScenarioParam param=ScenarioParam.builder()
                .scenario(scenario)
                .paramKey(request.getParamKey())
                .dataType(request.getDataType())
                .build();

        Rule rule;

        if(request.getRuleCode()!=null){
            rule=ruleRepo.findByRuleCode(request.getRuleCode()).orElseThrow(()-> new ResourceNotFoundException("Rule Not Found",new Rule()));
            param.setRule(rule);
        }
        param.setValidFrom(LocalDateTime.now());

        try{
            switch (request.getDataType()){
                case INT -> {
                    param.setIntValue(Long.parseLong(request.getValue()));
                }
                case DECIMAL -> {
                    param.setDecimalValue(new BigDecimal(request.getValue()));
                }
                default -> {
                    param.setStringValue(request.getValue());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value");
        }

        return param;
    }

}
