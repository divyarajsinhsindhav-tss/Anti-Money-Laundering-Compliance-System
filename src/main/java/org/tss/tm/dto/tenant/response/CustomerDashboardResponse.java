package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardResponse {
    private long totalCustomers;
    private long totalErrors;
    private long totalBatches;
    private List<RecentErrorResponse> recentErrors;
    private List<RecentJobResponse> recentJobs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentErrorResponse {
        private Long id;
        private String cif;
        private String errorMessage;
        private String errorCode;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentJobResponse {
        private String id;
        private String fileName;
        private String status;
        private Integer errorCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
