package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.ErrorSeverity;
import org.tss.tm.entity.tenant.RuleEngineError;
import org.tss.tm.repository.RuleEngineErrorRepo;
import org.tss.tm.service.interfaces.ScenarioParamService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutor {

    private final AmlExecutionEngine engine;
    private final ScenarioParamService paramService;
    private final RuleEngineErrorRepo ruleEngineErrorRepo;

    public void runScenario(AmlScenarioBlueprint blueprint, UUID jobId, LocalDate fromDate, LocalDate toDate) {

        Map<String, Map<String, Object>> nestedParams = paramService.getParams(blueprint.getScenarioId());

        Map<String, Object> commonParams = nestedParams.get("COMMON");
        if (commonParams == null) {
            RuleEngineError ruleEngineError=RuleEngineError.builder().info("Parameter Failure: No common parameter found").scenarioCode(blueprint.getScenarioCode()).jobId(String.valueOf(jobId)).severity(ErrorSeverity.MEDIUM).build();
            ruleEngineErrorRepo.save(ruleEngineError);
            throw new IllegalArgumentException("COMMON keys not found");
        }

        long lookbackDays = (long) commonParams.getOrDefault("LOOKBACK_DAYS", 0);
        long lookbackWindow = ChronoUnit.DAYS.between(fromDate,toDate)+1;

        if (lookbackDays <= 0) {
            RuleEngineError ruleEngineError=RuleEngineError.builder().info("Parameter Failure: Negative value of look back days").scenarioCode(blueprint.getScenarioCode()).jobId(String.valueOf(jobId)).severity(ErrorSeverity.MEDIUM).build();
            ruleEngineErrorRepo.save(ruleEngineError);
            throw new IllegalArgumentException("Days must be positive integer values.");
        }

        if (lookbackWindow < lookbackDays) {
            RuleEngineError ruleEngineError=RuleEngineError.builder().info("Warning: Minimum "+lookbackDays+" required, provided: "+lookbackWindow).scenarioCode(blueprint.getScenarioCode()).jobId(String.valueOf(jobId)).severity(ErrorSeverity.HIGH).build();
            ruleEngineErrorRepo.save(ruleEngineError);
                log.warn("Partial execution: Minimum required {} days, provided {}", lookbackDays, lookbackWindow);

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint, nestedParams, toDate, jobId, lookbackWindow);
            } else {
                engine.executeSingleTxnScenario(blueprint, nestedParams, toDate, jobId);
            }
            return;
        }

        long totalWindows = lookbackWindow - lookbackDays+1;

        for (int i = 0; i < totalWindows; i++) {
            LocalDate slidingToDate = toDate.minusDays(i);

            if (blueprint.isAggregateScenario()) {
                engine.executeMultipleTxnScenario(blueprint, nestedParams, slidingToDate, jobId, lookbackDays);
            } else {
                engine.executeSingleTxnScenario(blueprint, nestedParams, slidingToDate, jobId);
            }
        }
    }
}