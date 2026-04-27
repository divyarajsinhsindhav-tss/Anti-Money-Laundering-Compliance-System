package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tss.tm.entity.system.ScenarioParameterMaster;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScenarioParameterMasterRepo extends JpaRepository<ScenarioParameterMaster, UUID> {
    List<ScenarioParameterMaster> findByScenario_ScenarioId(UUID scenarioId);
    List<ScenarioParameterMaster> findByScenario_ScenarioIdAndRule_RuleId(UUID scenarioId, UUID ruleId);
}
