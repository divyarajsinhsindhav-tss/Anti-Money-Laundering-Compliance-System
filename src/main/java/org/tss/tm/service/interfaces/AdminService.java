package org.tss.tm.service.interfaces;

import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.dto.admin.request.AssignScenarioRequest;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.tss.tm.dto.admin.response.DashboardStatsResponse;

public interface AdminService {
    void assignScenario(AssignScenarioRequest assignScenarioRequest);

    Page<ScenarioResponse> getScenarios(Pageable pageable);

    ScenarioDetailResponse getScenario(String code);

    DashboardStatsResponse getDashboardStats();

    Page<DashboardStatsResponse.RecentJobResponse> getJobRecords(JobStatus status, String tenantCode, Pageable pageable);
}
