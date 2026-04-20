package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.FinancialTransaction;
import org.tss.tm.entity.tenant.ScenarioParam;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "rule")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_code")
    private String ruleCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rule_scenario",
            joinColumns = @JoinColumn(name = "rule_id"),
            inverseJoinColumns = @JoinColumn(name = "scenario_id")
    )
    private List<Scenario> scenarios;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_category")
    private String ruleCategory;

    @Column(name = "status")
    private String status;

    @JoinColumn(name = "created_by")
    @ManyToOne(fetch = FetchType.LAZY)
    private SystemAdmin createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "scenarioParam", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ScenarioParam> scenarioParams;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Alert> alerts;
}