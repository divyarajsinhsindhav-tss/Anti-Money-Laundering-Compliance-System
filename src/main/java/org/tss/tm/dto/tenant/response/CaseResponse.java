package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.CaseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponse {
    private String caseCode;
    private String createdByEmail;
    private String assignedToUserCode;
    private CaseStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
