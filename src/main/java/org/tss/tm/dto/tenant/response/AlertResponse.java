package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String alertCode;
    private String scenarioName;
    private String customerName;
    private String customerCode;
    private BigDecimal customerIncome;
    private AlertStatus alertStatus;
    private LocalDateTime createdAt;
}
