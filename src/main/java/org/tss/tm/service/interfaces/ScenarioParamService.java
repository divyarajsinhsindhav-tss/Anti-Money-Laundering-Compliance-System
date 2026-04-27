package org.tss.tm.service.interfaces;

import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.ScenarioParameterMaster;
import org.tss.tm.entity.system.Tenant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ScenarioParamService {
    Map<String, Map<String,Object>> getParams(UUID scenarioId);
    void createParametersFromMaster(Scenario scenario, List<ScenarioParameterMaster> masterParams);
}
