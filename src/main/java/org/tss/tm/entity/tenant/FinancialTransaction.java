package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.common.enums.TransactionDirection;
import org.tss.tm.common.enums.TransactionType;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.system.JobExecution;
import org.tss.tm.entity.system.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "financial_transaction")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private JobExecution job;

    @Column(name = "txn_no", nullable = false)
    private String txnNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false)
    private TransactionType txnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirection direction;

    @Column(name = "counterparty_account_no", length = 30)
    private String counterpartyAccountNo;

    @Column(name = "counterparty_bank_ifsc", length = 11)
    private String counterpartyBankIfsc;

    @Column(name = "txn_timestamp", nullable = false)
    private LocalDateTime txnTimestamp;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<Alert> alerts;
}