package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.common.enums.RuleCategory;
import org.tss.tm.common.enums.StatusBasic;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rules", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rule_id")
    @ToString.Include
    private UUID ruleId;

    @Column(name = "rule_code", unique = true, nullable = false)
    @ToString.Include
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    @ToString.Include
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rule_category", nullable = false, columnDefinition = "rule_category_enum")
    private RuleCategory ruleCategory;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "status_basic")
    private StatusBasic status = StatusBasic.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private SystemAdmin createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "scenario_rule_mapping", schema = "public", joinColumns = @JoinColumn(name = "rule_id"), inverseJoinColumns = @JoinColumn(name = "scenario_id"))
    private List<Scenario> scenarios;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioParam> scenarioParams;

    @OneToMany(mappedBy = "rule")
    private List<Alert> alerts;
}