package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "customer_errors")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "cif")
    private String cif;

    @Column(name = "staging_id")
    private Long stagingId;

    @Column(name = "raw_row", columnDefinition = "TEXT")
    private String rawRow;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "critical_errors", columnDefinition = "text[]")
    private List<String> criticalErrors;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "warning_errors", columnDefinition = "text[]")
    private List<String> warningErrors;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
