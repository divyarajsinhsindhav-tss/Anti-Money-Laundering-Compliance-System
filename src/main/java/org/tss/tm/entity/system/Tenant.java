package org.tss.tm.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.tss.tm.entity.tenant.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "tenant")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "tenant_code")
    private String tenantCode;

    @Column(name = "name")
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "status")
    private String status;

    @JoinColumn(name = "onboarded_by_admin_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private SystemAdmin onboardedByAdmin;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<JobExecution> jobExecutions;

    @OneToMany(mappedBy = "account", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Account> accounts;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Alert> alerts;

    @OneToMany(mappedBy = "amlCase", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AmlCase> amlCases;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Customer> customers;

    @OneToMany(mappedBy = "financialTransaction", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<FinancialTransaction> transactions;

    @OneToMany(mappedBy = "scenarioParam", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ScenarioParam> scenarioParams;

    @OneToMany(mappedBy = "tenantUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<TenantUser> tenantUsers;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantScenarioMapping> tenantScenarios;
}