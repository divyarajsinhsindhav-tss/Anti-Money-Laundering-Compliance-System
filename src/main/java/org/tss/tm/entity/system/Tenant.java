package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tss.tm.entity.common.BaseEntity;
import org.tss.tm.common.enums.TenantStatus;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tenant", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tenant_id")
    @ToString.Include
    private UUID tenantId;

    @Column(name = "tenant_code", unique = true, nullable = false)
    @ToString.Include
    private String tenantCode;

    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "display_name", nullable = false)
    @ToString.Include
    private String displayName;

    @Column(name = "schema_name", unique = true, nullable = false)
    @ToString.Include
    private String schemaName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "tenant_status_enum")
    private TenantStatus status = TenantStatus.ONBOARDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarded_by_admin_id", nullable = false)
    private SystemAdmin onboardedByAdmin;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<JobRecord> jobRecords;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantScenarioMapping> tenantScenarios;
}