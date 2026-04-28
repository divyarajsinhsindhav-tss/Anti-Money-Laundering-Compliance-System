package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlExecutor {

    private final AmlExecutionEngine engine;

    public void runScenario(AmlScenarioBlueprint blueprint, int lookbackDays, int adminDays, UUID jobId) {

        if (adminDays <= 0 || lookbackDays <= 0) {
            throw new IllegalArgumentException("Days must be positive integer values.");
        }

        LocalDate today = LocalDate.now();
        if (adminDays <= lookbackDays) {

            int effectiveLookback = Math.min(adminDays, lookbackDays);

            if (adminDays < lookbackDays) {
                log.warn("Partial execution: required {} days, provided {}. Engine will use effective lookback of {}", lookbackDays, adminDays, effectiveLookback);
            } else {
                log.info("Snapshot execution for exactly {} days.", lookbackDays);
            }

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint, today, jobId, effectiveLookback);
            } else {
                engine.executeSingleTxnScenario(blueprint, today, jobId);
            }
            return;
        }

        int totalWindows = adminDays - lookbackDays + 1;

        for (int i = 0; i < totalWindows; i++) {
            LocalDate anchor = today.minusDays(i);

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint, anchor, jobId, lookbackDays);
            } else {
                engine.executeSingleTxnScenario(blueprint, anchor, jobId);
            }
        }
    }
}