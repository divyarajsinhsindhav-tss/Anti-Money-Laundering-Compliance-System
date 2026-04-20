package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scenario_param")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScenarioParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_param_id")
    private Long scenarioParamId;

    @JoinColumn(name = "tenant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tenant tenant;

    @JoinColumn(name = "scenario_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Scenario scenario;

    @JoinColumn(name = "rule_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Rule rule;

    @Column(name = "param_key")
    private String paramKey;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "string_value", columnDefinition = "TEXT")
    private String stringValue;

    @Column(name = "int_value")
    private Long intValue;

    @Column(name = "decimal_value")
    private BigDecimal decimalValue;

    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}