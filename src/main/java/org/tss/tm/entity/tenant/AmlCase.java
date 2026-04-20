package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.system.Tenant;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "aml_case")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AmlCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long caseId;

    @JoinColumn(name = "tenant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Tenant tenant;

    @Column(name = "case_code")
    private String caseCode;

    @JoinColumn(name = "created_by")
    @ManyToOne(fetch = FetchType.LAZY)
    private TenantUser createdBy;

    @JoinColumn(name = "assigned_to")
    @ManyToOne(fetch = FetchType.LAZY)
    private TenantUser assignedTo;

    @Column(name = "status")
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}