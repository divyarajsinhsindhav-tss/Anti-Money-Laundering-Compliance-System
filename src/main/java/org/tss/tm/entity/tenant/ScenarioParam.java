package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.common.enums.DataType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "scenario_param")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioParam {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scenario_param_id")
    @ToString.Include
    private UUID scenarioParamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = true)
    private Rule rule;

    @Column(name = "param_key", nullable = false)
    @ToString.Include
    private String paramKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "data_type", nullable = false, columnDefinition = "data_type_enum")
    private DataType dataType;

    @Column(name = "string_value", columnDefinition = "TEXT")
    private String stringValue;

    @Column(name = "int_value")
    private Long intValue;

    @Column(name = "decimal_value", precision = 20, scale = 6)
    private BigDecimal decimalValue;
//
//    @Builder.Default
//    @Column(name = "version", nullable = false)
//    private Integer version = 1;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;
}