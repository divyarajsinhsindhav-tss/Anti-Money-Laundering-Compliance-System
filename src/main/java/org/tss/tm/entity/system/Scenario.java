package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.common.enums.StatusBasic;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "scenarios", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "scenario_id")
    @ToString.Include
    private UUID scenarioId;

    @Column(name = "scenario_code", unique = true, nullable = false)
    @ToString.Include
    private String scenarioCode;

    @Column(name = "scenario_name", nullable = false)
    @ToString.Include
    private String scenarioName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "status_basic")
    private StatusBasic status = StatusBasic.ACTIVE;

    @ManyToMany(mappedBy = "scenarios", fetch = FetchType.LAZY)
    private List<Rule> rules;

    // @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private List<ScenarioParam> scenarioParams;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantScenarioMapping> tenantScenarios;
}