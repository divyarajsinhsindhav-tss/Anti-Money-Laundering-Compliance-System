package org.tss.tm.dto.tenant.response;

import lombok.*;
import org.tss.tm.entity.tenant.FinancialTransaction;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDetailResponse {
    private AlertResponse alert;
    private List<FinancialTransactionResponse> financialTransactionResponsesList;
    private List<CustomerResponse> customerResponsesList;
}
