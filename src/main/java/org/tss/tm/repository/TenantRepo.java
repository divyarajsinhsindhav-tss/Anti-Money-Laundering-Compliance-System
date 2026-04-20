package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Tenant;

import java.util.List;

public interface TenantRepo extends JpaRepository<Tenant, Long> {
    Tenant findByName(String name);
}
