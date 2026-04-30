package org.tss.tm.dto.tenant.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.DataType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioParamUploadRequest {
    private String ruleCode;

    @NotEmpty(message = "Scenario Code can not be empty")
    private String scenarioCode;

    @NotEmpty(message = "Pram Key can not be empty")
    private String paramKey;

    @NotEmpty(message = "Data Type can not be empty")
    private DataType dataType;

    private String value;
}
