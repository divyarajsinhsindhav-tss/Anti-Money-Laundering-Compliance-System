package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tss.tm.entity.tenant.CustomerError;

@Repository
public interface CustomerErrorRepo extends JpaRepository<CustomerError, Long> {
    Page<CustomerError> findByJobId(String jobId, Pageable pageable);
}
