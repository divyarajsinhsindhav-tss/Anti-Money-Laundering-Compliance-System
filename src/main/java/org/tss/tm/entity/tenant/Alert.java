package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.system.*;
import org.tss.tm.common.enums.AlertStatus;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "alerts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "alert_code", nullable = false, unique = true)
    private String alertCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobRecord job;
    //
    // @Column(name = "rule_param_version", nullable = false)
    // private Integer ruleParamVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertInfo> alertInfos;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "alert_status", nullable = false, columnDefinition = "alert_status_enum")
    private AlertStatus alertStatus = AlertStatus.OPEN;
}