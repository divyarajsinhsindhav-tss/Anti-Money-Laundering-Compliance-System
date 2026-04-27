package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlJobOrchestrator {

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

            engine.executeScenario(blueprint, today, jobId, effectiveLookback);
            return;
        }

        log.info("Historical replay: {} days requested with rule lookback {}", adminDays, lookbackDays);

        int totalWindows = adminDays - lookbackDays + 1;

        for (int i = 0; i < totalWindows; i++) {
            LocalDate anchor = today.minusDays(i);
            engine.executeScenario(blueprint, anchor, jobId, lookbackDays);
        }
    }
}