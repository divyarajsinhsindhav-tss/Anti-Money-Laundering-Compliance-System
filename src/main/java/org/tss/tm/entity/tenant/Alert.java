package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.system.JobExecution;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.Tenant;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "alert")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @JoinColumn(name = "tenant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tenant tenant;

    @Column(name = "alert_code")
    private String alertCode;

    @JoinColumn(name = "job_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private JobExecution job;

    @JoinColumn(name = "rule_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Rule rule;

    @Column(name = "rule_param_version")
    private Integer ruleParamVersion;

    @Column(name = "scenario_id")
    @OneToMany(fetch = FetchType.LAZY)
    private Scenario scenario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "alert_transaction",
            joinColumns = @JoinColumn(name = "alert_id"),
            inverseJoinColumns = @JoinColumn(name = "transaction_id")
    )
    private List<FinancialTransaction> transactions;

    @JoinColumn(name = "customer_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @Column(name = "alert_status")
    private String alertStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}