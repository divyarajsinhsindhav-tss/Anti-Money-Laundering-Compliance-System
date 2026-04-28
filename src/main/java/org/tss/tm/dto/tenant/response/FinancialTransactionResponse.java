package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialTransactionResponse {
    private String txnNo;
    private String accountNo;
    private BigDecimal amount;
    private TransactionType txnType;
    private TransactionDirection direction;
    private String counterpartyAccountNo;
    private String counterpartyBankIfsc;
    private LocalDateTime txnTimestamp;
    private String countryCode;
}
