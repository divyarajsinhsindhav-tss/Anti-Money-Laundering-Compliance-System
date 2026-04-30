package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Tenant;

import java.util.List;
import java.util.UUID;

public interface JobRepo extends JpaRepository<JobRecord, UUID> {
    List<JobRecord> findByTenantOrderByCreatedAtDesc(Tenant tenant);
    long countByTenant(Tenant tenant);
    long countByStatus(JobStatus status);
    @Query("SELECT j FROM JobRecord j JOIN FETCH j.tenant ORDER BY j.createdAt DESC")
    List<JobRecord> findRecentJobs(Pageable pageable);

    @Query("""
           SELECT j FROM JobRecord j JOIN FETCH j.tenant 
           WHERE (CAST(:status AS string) IS NULL OR j.status = :status)
           AND (CAST(:tenantCode AS string) IS NULL OR j.tenant.tenantCode = :tenantCode)
           ORDER BY j.createdAt DESC
           """)
    Page<JobRecord> findAllJobs(@Param("status") JobStatus status, @Param("tenantCode") String tenantCode, Pageable pageable);
}
