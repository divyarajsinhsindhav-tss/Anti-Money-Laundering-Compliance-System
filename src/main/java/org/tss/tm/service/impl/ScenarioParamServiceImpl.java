package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.exception.ResourceNotFoundException;
import org.tss.tm.repository.ScenarioParamRepo;
import org.tss.tm.repository.ScenarioRepo;
import org.tss.tm.service.interfaces.ScenarioParamService;

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
            String ruleCode = param.getRule().getRuleCode();
            String paramKey = param.getParamKey().toUpperCase();

            Map<String, Object> ruleParams = params.computeIfAbsent(ruleCode, k -> new HashMap<>());

            switch (param.getDataType()){
                case INT -> ruleParams.put(paramKey, param.getIntValue());
                case DECIMAL -> ruleParams.put(paramKey, param.getDecimalValue());
                default -> ruleParams.put(paramKey, param.getStringValue());
            }
        }

        return params;
    }
}
