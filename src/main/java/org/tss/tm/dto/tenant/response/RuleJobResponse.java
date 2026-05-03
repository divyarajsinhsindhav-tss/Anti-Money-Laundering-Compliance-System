package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class RuleJobResponse {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentJobResponse {
        private String id;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String jobType;
    }
}
