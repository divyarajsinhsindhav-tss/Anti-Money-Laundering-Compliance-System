package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;
import org.tss.tm.entity.system.JobExecution;
import org.tss.tm.entity.system.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "financial_transaction")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @JoinColumn(name = "tenant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tenant tenant;

    @JoinColumn(name = "job_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private JobExecution job;

    @Column(name = "txn_no")
    private String txnNo;

    @JoinColumn(name = "account_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Account account;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type")
    private TransactionType txnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private TransactionDirection direction;

    @Column(name = "counterparty_account_no")
    private String counterpartyAccountNo;

    @Column(name = "counterparty_bank_ifsc")
    private String counterpartyBankIfsc;

    @Column(name = "txn_timestamp")
    private LocalDateTime txnTimestamp;

    @Column(name = "swift_code")
    private String swiftCode;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "transactions")
    private List<Alert> alerts;
}