package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.Scenario;

import java.util.Optional;
import java.util.UUID;

public interface ScenarioRepo extends JpaRepository<Scenario, UUID> {
    Optional<Scenario> findScenarioByScenarioCode(String tenantCode);
}
