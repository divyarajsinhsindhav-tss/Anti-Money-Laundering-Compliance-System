package org.tss.tm.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AssignScenarioRequest {

    @NotBlank(message = "")
    private String scenarioCode;

    @NotBlank(message = "")
    private String tenantCode;
}
