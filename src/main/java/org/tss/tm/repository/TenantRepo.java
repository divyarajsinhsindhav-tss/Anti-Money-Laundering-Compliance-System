package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepo extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByName(String name);
    Optional<Tenant> findBySchemaName(String schemaName);
    boolean existsTenantByTenantCode(String tenantCode);
    Optional<Tenant> findByTenantCode(String tenantCode);
    Optional<Tenant> findTenantBySchemaName(String schemaName);
}