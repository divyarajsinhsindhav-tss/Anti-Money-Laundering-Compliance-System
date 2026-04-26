package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.entity.system.Tenant;

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
    @Column(name = "status_from", nullable = false)
    private AlertStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false)
    private AlertStatus statusTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private TenantUser changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "reason")
    private String reason;
}
