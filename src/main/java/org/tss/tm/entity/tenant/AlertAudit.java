package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.common.enums.AlertStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "alert_audits")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_audit_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_from", nullable = false, columnDefinition = "alert_status_enum")
    private AlertStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_to", nullable = false, columnDefinition = "alert_status_enum")
    private AlertStatus statusTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private TenantUser changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "reason")
    private String reason;
}
