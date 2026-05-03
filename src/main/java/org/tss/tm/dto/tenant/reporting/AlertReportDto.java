package org.tss.tm.dto.tenant.reporting;

import org.tss.tm.common.enums.AlertStatus;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertReportDto {

    private String alertCode;
    private ScenarioReportDto scenario;
    private AlertStatus alertStatus;
    private LocalDateTime createdAt;
    private List<RuleReportDto> rulesBroken;
    private List<TransactionReportDto> involvedTransactions;
}
