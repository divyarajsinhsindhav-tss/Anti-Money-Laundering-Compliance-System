package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.AccountType;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailResponse {
    private UUID transactionId;
    private String txnNo;
    private BigDecimal amount;
    private TransactionType txnType;
    private TransactionDirection direction;
    private String counterpartyAccountNo;
    private String counterpartyBankIfsc;
    private LocalDateTime txnTimestamp;
    private String countryCode;

    private AccountInfo account;
    private CustomerInfo customer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private UUID accountId;
        private String accountNumber;
        private AccountType accountType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private UUID customerId;
        private String cif;
        private String fullName;
    }
}
