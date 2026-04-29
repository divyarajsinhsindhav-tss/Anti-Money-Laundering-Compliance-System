package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tss.tm.service.interfaces.ScenarioParamService;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutor {

    private final AmlExecutionEngine engine;
    private final ScenarioParamService paramService;

    public void runScenario(AmlScenarioBlueprint blueprint, UUID jobId) {

        Map<String, Map<String, Object>> nestedParams=paramService.getParams(blueprint.getScenarioId());

        Map<String, Object> commonParams = nestedParams.get("COMMON");
        if (commonParams == null) {
            throw new IllegalArgumentException("COMMON key not found");
        }

        int lookbackDays= (int) commonParams.getOrDefault("LOOKBACK_DAYS",0);
        int lookbackWindow=(int) commonParams.getOrDefault("LOOKBACK_WINDOW",0);

        if (lookbackWindow <= 0 || lookbackDays <= 0) {
            throw new IllegalArgumentException("Days must be positive integer values.");
        }

        LocalDate today = LocalDate.now();
        if (lookbackWindow <= lookbackDays) {

            int effectiveLookback = Math.min(lookbackWindow, lookbackDays);

            if (lookbackWindow < lookbackDays) {
                log.warn("Partial execution: required {} days, provided {}. Engine will use effective lookback of {}", lookbackDays, lookbackWindow, effectiveLookback);
            } else {
                log.info("Snapshot execution for exactly {} days.", lookbackDays);
            }

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint,nestedParams, today, jobId, effectiveLookback);
            } else {
                engine.executeSingleTxnScenario(blueprint,nestedParams, today, jobId);
            }
            return;
        }

        int totalWindows = lookbackWindow - lookbackDays + 1;

        for (int i = 0; i < totalWindows; i++) {
            LocalDate anchor = today.minusDays(i);

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint,nestedParams, today, jobId, lookbackDays);
            } else {
                engine.executeSingleTxnScenario(blueprint,nestedParams, anchor, jobId);
            }
        }
    }
}