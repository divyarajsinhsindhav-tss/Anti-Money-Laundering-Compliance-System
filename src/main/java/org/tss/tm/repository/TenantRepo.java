package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepo extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByName(String name);
    Optional<Tenant> findByTenantCode(String code);
}
