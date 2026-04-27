package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tss.tm.entity.tenant.ScenarioParam;

import java.util.List;
import java.util.UUID;

public interface ScenarioParamRepo extends JpaRepository<ScenarioParam, UUID> {
    @Query("SELECT p FROM ScenarioParam p JOIN FETCH p.rule WHERE p.scenario.scenarioId = :scenarioId AND p.validTo IS NULL")
    List<ScenarioParam> findActiveParametersForScenario(UUID scenarioId);
}
