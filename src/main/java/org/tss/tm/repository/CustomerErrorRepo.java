package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tss.tm.entity.tenant.CustomerError;

import java.util.List;

@Repository
public interface CustomerErrorRepo extends JpaRepository<CustomerError, Long> {
    List<CustomerError> findByJobId(String jobId);
}
