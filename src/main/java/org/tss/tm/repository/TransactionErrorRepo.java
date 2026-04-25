package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tss.tm.entity.tenant.TransactionError;

import java.util.List;

@Repository
public interface TransactionErrorRepo extends JpaRepository<TransactionError, Long> {
    List<TransactionError> findByJobId(String jobId);
}
