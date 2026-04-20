package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.tenant.ScenarioParam;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "scenario")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Column(name = "scenario_code")
    private String scenarioCode;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantScenarioMapping> tenantScenarios;
}