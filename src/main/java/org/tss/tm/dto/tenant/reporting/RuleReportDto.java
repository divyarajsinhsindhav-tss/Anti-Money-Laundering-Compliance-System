package org.tss.tm.dto.tenant.reporting;

import org.tss.tm.common.enums.RuleCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleReportDto {

    private String ruleCode;
    private String ruleName;
    private String description;
    private RuleCategory ruleCategory;
}
