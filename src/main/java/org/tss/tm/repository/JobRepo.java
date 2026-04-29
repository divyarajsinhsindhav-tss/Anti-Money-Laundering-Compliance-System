package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Tenant;

import java.util.List;
import java.util.UUID;

public interface JobRepo extends JpaRepository<JobRecord, UUID> {
    List<JobRecord> findByTenantOrderByCreatedAtDesc(Tenant tenant);
    long countByTenant(Tenant tenant);
}
