package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.system.TenantScenarioMapping;

import java.util.UUID;

public interface TenantScenarioRepo extends JpaRepository<TenantScenarioMapping, UUID> {
    Page<TenantScenarioMapping> findAllByTenant(Tenant tenant, Pageable pageable);
}
