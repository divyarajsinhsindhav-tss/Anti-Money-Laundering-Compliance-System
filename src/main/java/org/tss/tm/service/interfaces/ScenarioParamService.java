package org.tss.tm.service.interfaces;

import java.util.Map;
import java.util.UUID;

public interface ScenarioParamService {
    Map<String, Map<String,Object>> getParams(UUID scenarioId);
}
