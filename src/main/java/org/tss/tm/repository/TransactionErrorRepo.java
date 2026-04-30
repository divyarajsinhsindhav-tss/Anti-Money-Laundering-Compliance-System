package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tss.tm.entity.tenant.TransactionError;

import java.util.List;

@Repository
public interface TransactionErrorRepo extends JpaRepository<TransactionError, Long> {
    Page<TransactionError> findByJobId(String jobId, Pageable pageable);
}
