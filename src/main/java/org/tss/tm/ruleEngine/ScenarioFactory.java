package org.tss.tm.ruleEngine;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScenarioFactory {

    private final Map<String, AmlScenarioBlueprint> blueprintMap;

    public ScenarioFactory(List<AmlScenarioBlueprint> blueprints) {
        this.blueprintMap = blueprints.stream().collect(Collectors.toMap(
                b -> b.getScenarioCode().toUpperCase(),
                b -> b
        ));
    }

    public AmlScenarioBlueprint getBlueprint(String scenarioCode) {
        AmlScenarioBlueprint blueprint = blueprintMap.get(scenarioCode.toUpperCase());

        if (blueprint == null) {
            throw new IllegalArgumentException("Unsupported Scenario Code: " + scenarioCode);
        }

        return blueprint;
    }
}