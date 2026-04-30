package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.ErrorSeverity;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rule_engine_errors")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEngineError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "info", columnDefinition = "TEXT")
    private String info;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private ErrorSeverity severity;

    @Column(name = "rule_code")
    private String ruleCode;

    @Column(name = "scenario_code")
    private String scenarioCode;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
