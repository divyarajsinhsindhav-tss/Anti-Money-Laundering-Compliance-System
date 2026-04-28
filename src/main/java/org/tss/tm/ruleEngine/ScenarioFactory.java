package org.tss.tm.ruleEngine;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScenarioFactory {

    public AmlScenarioBlueprint getBlueprint(String scenarioCode, UUID scenarioId) {

        return switch (scenarioCode.toUpperCase()) {
            case "PASS_THROUGH" -> new PassThroughScenarioBlueprint(scenarioId, new JdbcTemplate());

            default -> throw new IllegalArgumentException("Unsupported Scenario Code found in database: " + scenarioCode);
        };
    }
}