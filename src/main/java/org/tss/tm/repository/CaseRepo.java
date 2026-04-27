package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.entity.tenant.AmlCase;

import java.util.UUID;

public interface CaseRepo extends JpaRepository<AmlCase, UUID> {
    Page<AmlCase> findAllByStatus(CaseStatus status, Pageable pageable);
    Page<AmlCase> findAllByAssignedTo_Email(String email, Pageable pageable);
    Page<AmlCase> findAllByStatusAndAssignedTo_Email(CaseStatus status, String email, Pageable pageable);
}
