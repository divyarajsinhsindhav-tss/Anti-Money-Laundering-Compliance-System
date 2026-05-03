package org.tss.tm.dto.tenant.reporting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioReportDto {

    private String scenarioCode;
    private String scenarioName;
    private String description;
}
