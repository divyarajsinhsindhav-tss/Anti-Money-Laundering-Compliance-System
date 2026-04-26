package org.tss.tm.service.interfaces;

import org.tss.tm.dto.admin.request.AssignScenarioRequest;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    void assignScenario(AssignScenarioRequest assignScenarioRequest);

    Page<ScenarioResponse> getScenarios(Pageable pageable);

    ScenarioDetailResponse getScenario(String code);
}
