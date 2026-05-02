package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.request.ScenarioParamUploadRequest;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.ScenarioParameterMaster;
import org.tss.tm.dto.tenant.response.ScenarioParamResponse;
import org.tss.tm.entity.tenant.ScenarioParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ParamService {
    Map<String, Map<String,Object>> getParams(UUID scenarioId);
    void createParametersFromMaster(Scenario scenario, List<ScenarioParameterMaster> masterParams);
    ScenarioParam convertToEntity(ScenarioParamUploadRequest request);
    void updateScenarioParams(ScenarioParamUploadRequest request);
    List<ScenarioParamResponse> getAllScenarioParams();
}

