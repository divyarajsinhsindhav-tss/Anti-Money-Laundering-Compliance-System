package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionListResponse {
    private UUID transactionId;
    private String txnNo;
    private String accountNumber;
    private String customerName;
    private BigDecimal amount;
    private TransactionType txnType;
    private TransactionDirection direction;
    private LocalDateTime txnTimestamp;
}
