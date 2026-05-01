package org.tss.tm.dto.tenant.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.tss.tm.common.enums.CaseStatus;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCaseStatusRequest {

    @NotNull(message = "Case status can't be null")
    private CaseStatus caseStatus;

    private String reason;

}
