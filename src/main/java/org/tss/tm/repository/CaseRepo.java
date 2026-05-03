package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.entity.tenant.AmlCase;

import java.util.UUID;
import java.util.Optional;

public interface CaseRepo extends JpaRepository<AmlCase, UUID> {
    Page<AmlCase> findAllByStatus(CaseStatus status, Pageable pageable);

    Page<AmlCase> findAllByAssignedTo_Email(String email, Pageable pageable);

    Page<AmlCase> findAllByStatusAndAssignedTo_Email(CaseStatus status, String email, Pageable pageable);

    @EntityGraph(attributePaths = { "alerts", "alerts.customer", "alerts.scenario" })
    Optional<AmlCase> findByCaseCode(String caseCode);

    long countByAssignedTo_UserCode(String userCode);

    @EntityGraph(attributePaths = {
            "createdBy",
            "assignedTo",
            "alerts",
            "alerts.customer",
            "alerts.scenario"
    })
    Optional<AmlCase> findDetailedByCaseCode(String caseCode);

    long countByStatus(CaseStatus status);
}
