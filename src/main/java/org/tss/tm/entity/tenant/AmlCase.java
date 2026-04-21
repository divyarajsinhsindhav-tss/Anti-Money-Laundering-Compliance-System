package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.system.Tenant;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "aml_case")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "case_id")
    private UUID caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "case_code", nullable = false, length = 20)
    private String caseCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private TenantUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private TenantUser assignedTo;

    @Column(name = "status", nullable = false)
    private String status; // Consider using CaseStatus enum

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}