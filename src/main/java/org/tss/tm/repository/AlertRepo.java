package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.entity.tenant.Alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepo extends JpaRepository<Alert, UUID> {
    List<Alert> findAllByAlertCodeIn(List<String> alertCodes);

    Page<Alert> findAllByAlertStatus(AlertStatus status, Pageable pageable);

    List<Alert> findAllByAlertStatus(AlertStatus status);

    Optional<Alert> findByAlertCode(String alertCode);

    Optional<Alert> findByCustomer_CustomerIdAndScenario_ScenarioIdAndAlertStatus(
            UUID customerId, UUID scenarioId, AlertStatus status);

    Page<Alert> findAllByAlertCodeContainingIgnoreCase(String alertCode, Pageable pageable);

    Page<Alert> findAllByAlertCodeContainingIgnoreCaseAndAlertStatus(String alertCode, AlertStatus status,
            Pageable pageable);

    Page<Alert> findAllByAmlCase_AssignedTo_Email(String email, Pageable pageable);

    Page<Alert> findAllByAmlCase_AssignedTo_EmailAndAlertCodeContainingIgnoreCase(String email, String alertCode, Pageable pageable);

    Page<Alert> findAllByAmlCase_AssignedTo_EmailAndAlertStatus(String email, AlertStatus status, Pageable pageable);

    Page<Alert> findAllByAmlCase_AssignedTo_EmailAndAlertCodeContainingIgnoreCaseAndAlertStatus(String email, String alertCode, AlertStatus status,
                                                                                               Pageable pageable);

    List<Alert> findAllByAmlCase_CaseCode(String caseCode);

    long countByAlertStatus(AlertStatus status);
}
