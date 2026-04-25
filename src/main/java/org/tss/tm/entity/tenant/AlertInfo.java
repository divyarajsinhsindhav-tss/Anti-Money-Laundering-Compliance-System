package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "alert_info",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"transaction_id", "rule_id", "scenario_id"})
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_info_id")
    private UUID alertInfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private FinancialTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;
}