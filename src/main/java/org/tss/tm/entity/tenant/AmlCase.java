package org.tss.tm.entity.tenant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.entity.system.Tenant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "aml_case")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "case_id")
    @ToString.Include
    private UUID caseId;

    @Column(name = "case_code", nullable = false, length = 20)
    @ToString.Include
    private String caseCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private TenantUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private TenantUser assignedTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "case_status_enum")
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "amlCase", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Alert> alerts;
}