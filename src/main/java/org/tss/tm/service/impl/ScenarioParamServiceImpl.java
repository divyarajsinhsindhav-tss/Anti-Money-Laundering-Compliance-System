package org.tss.tm.service.impl;

import org.tss.tm.service.interfaces.ScenarioParamService;

import java.util.Map;
import java.util.UUID;

public class ScenarioParamServiceImpl implements ScenarioParamService {
    @Override
    public Map<String, Map<String, Object>> getParams(UUID scenarioId) {
        return Map.of();
    }
}
