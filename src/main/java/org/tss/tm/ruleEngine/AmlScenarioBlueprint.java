package org.tss.tm.ruleEngine;
import java.util.List;
import java.util.UUID;

public interface AmlScenarioBlueprint {
    UUID getScenarioId();
    String getBaseSqlTemplate();
    String getLogicalOperator();
    List<AmlRule> getRules();
}
