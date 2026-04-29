package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecordResponse {
    private UUID jobId;
    private JobType jobType;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
