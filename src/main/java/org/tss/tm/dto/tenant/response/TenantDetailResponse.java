package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailResponse {
    private TenantResponse tenant;
    private List<ScenarioResponse> subscribedScenarios;
    private long jobRunCount;
    private List<JobRecordResponse> jobHistory;
}
