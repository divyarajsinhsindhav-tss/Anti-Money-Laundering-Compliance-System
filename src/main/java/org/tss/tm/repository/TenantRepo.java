package org.tss.tm.repository;

import java.util.UUID;

public interface TenantRepo extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByName(String name);
}
