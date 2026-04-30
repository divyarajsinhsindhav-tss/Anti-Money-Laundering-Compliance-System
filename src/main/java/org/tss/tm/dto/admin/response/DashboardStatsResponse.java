package org.tss.tm.dto.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalTenants;
    private long totalActiveScenarios;
    private String totalDbStorage;
    private long totalJobs;
    private long runningJobs;
    private long pendingJobs;
    private long failedJobs;
    private long completedJobs;
    private List<RecentJobResponse> recentJobs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentJobResponse {
        private UUID jobId;
        private String tenantName;
        private String tenantCode;
        private JobType jobType;
        private JobStatus status;
        private LocalDateTime createdAt;
    }
}
