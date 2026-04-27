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

    Optional<Alert> findByAlertCode(String alertCode);

    Optional<Alert> findByCustomer_CustomerIdAndScenario_ScenarioIdAndAlertStatus(
            UUID customerId, UUID scenarioId, AlertStatus status);
}
