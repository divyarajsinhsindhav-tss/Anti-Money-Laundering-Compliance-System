package org.tss.tm.ruleEngine;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScenarioFactory {

    public AmlScenarioBlueprint getBlueprint(String scenarioCode, UUID scenarioId) {

        return switch (scenarioCode.toUpperCase()) {
            case "S1_PASS_THROUGH" -> new PassThroughScenarioBlueprint(scenarioId);

            default -> throw new IllegalArgumentException("Unsupported Scenario Code found in database: " + scenarioCode);
        };
    }
}