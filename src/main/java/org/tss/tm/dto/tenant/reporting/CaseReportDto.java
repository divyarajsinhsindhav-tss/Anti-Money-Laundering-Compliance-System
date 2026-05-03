package org.tss.tm.dto.tenant.reporting;

import org.tss.tm.common.enums.CaseStatus;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseReportDto {

    private String caseCode;
    private TenantUserReportDto createdBy;
    private TenantUserReportDto assignedTo;
    private CaseStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
