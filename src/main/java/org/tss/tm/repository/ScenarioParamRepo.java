package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tss.tm.entity.tenant.ScenarioParam;

import java.util.List;
import java.util.UUID;

public interface ScenarioParamRepo extends JpaRepository<ScenarioParam, UUID> {
    @Query("SELECT p FROM ScenarioParam p JOIN FETCH p.rule WHERE p.scenario.scenarioId = :scenarioId AND p.validTo IS NULL")
    List<ScenarioParam> findActiveParametersForScenario(UUID scenarioId);

    @Query("SELECT p FROM ScenarioParam p WHERE p.scenario.scenarioId = :scenarioId AND p.rule.ruleId = :ruleId AND p.paramKey = :paramKey AND p.validTo IS NULL")
    ScenarioParam findScenarioRuleParam(@Param("scenarioId") UUID scenarioId, @Param("ruleId") UUID ruleId, @Param("paramKey") String paramKey);

    @Query("SELECT p FROM ScenarioParam p WHERE p.scenario.scenarioId = :scenarioId AND p.rule IS NULL AND p.paramKey = :paramKey AND p.validTo IS NULL")
    ScenarioParam findCommonScenarioParam(@Param("scenarioId") UUID scenarioId, @Param("paramKey") String paramKey);
}
