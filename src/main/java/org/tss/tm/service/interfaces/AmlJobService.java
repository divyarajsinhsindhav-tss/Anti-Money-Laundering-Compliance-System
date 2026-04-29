package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.response.RuleEngineResponse;

import java.util.UUID;

public interface AmlJobService {
    RuleEngineResponse executeTenantScenarios();
}
