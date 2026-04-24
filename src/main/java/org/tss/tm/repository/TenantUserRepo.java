package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.entity.tenant.TenantUser;

import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepo extends JpaRepository<TenantUser, UUID> {
    Optional<TenantUser> findByEmail(String email);
}
