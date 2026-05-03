package org.tss.tm.dto.tenant.reporting;

import lombok.*;
import org.tss.tm.common.enums.AccountType;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionReportDto {

    private String txnNo;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal amount;
    private TransactionType txnType;
    private TransactionDirection direction;
    private String counterpartyAccountNo;
    private String counterpartyBankIfsc;
    private LocalDateTime txnTimestamp;
    private String countryCode;
}