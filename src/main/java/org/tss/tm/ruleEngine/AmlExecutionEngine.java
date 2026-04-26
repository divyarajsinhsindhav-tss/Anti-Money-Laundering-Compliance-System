package org.tss.tm.ruleEngine;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.repository.*;
import org.tss.tm.service.interfaces.ScenarioParamService;

import java.util.*;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlExecutionEngine {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ScenarioParamService paramService;
    private final AlertRepo alertRepo;
    private final AlertInfoRepo alertInfoRepo;
    private final ScenarioRepo scenarioRepo;
    private final CustomerRepo customerRepo;
    private final JobRepo jobRepo;

    private record ExtractedCriminal(UUID customerId, UUID[] involvedTxns) {
    }

    @Transactional
    public void executeScenario(AmlScenarioBlueprint blueprint, int adminDays, UUID currentJobId) {

        if (blueprint.getRules().isEmpty()) return;

        Map<String, Map<String, Object>> nestedParams = paramService.getParams(blueprint.getScenarioId());
        Map<String, Object> flatSqlParams = new HashMap<>();

        Integer lookbackDays = null;

        for (Map<String, Object> paramMap : nestedParams.values()) {
            Object value = paramMap.get("LOOKBACK_DAYS");
            if (value != null) {
                lookbackDays = ((Number) value).intValue();
                break;
            }
        }

        if (lookbackDays == null) {
            throw new IllegalStateException("Missing LOOKBACK_DAYS parameter.");
        }

        flatSqlParams.put("LOOKBACK_DAYS", lookbackDays);

        for (Map.Entry<String, Map<String, Object>> ruleEntry : nestedParams.entrySet()) {
            String ruleCode = ruleEntry.getKey();
            for (Map.Entry<String, Object> paramEntry : ruleEntry.getValue().entrySet()) {
                flatSqlParams.put(ruleCode + "_" + paramEntry.getKey(), paramEntry.getValue());
            }
        }

        List<String> compiledConditions = new ArrayList<>();
        for (AmlRule rule : blueprint.getRules()) {
            Map<String, Object> ruleParams = nestedParams.getOrDefault(rule.getRuleCode(), Collections.emptyMap());
            compiledConditions.add(rule.getBooleanCondition(ruleParams));
        }

        String dynamicWhereClause = String.join(blueprint.getLogicalOperator(), compiledConditions);
        String finalSqlQuery = String.format(blueprint.getBaseSqlTemplate(), dynamicWhereClause);

        Scenario scenarioRef = scenarioRepo.getReferenceById(blueprint.getScenarioId());
        JobRecord jobRef = jobRepo.getReferenceById(currentJobId);

        String insertEvidenceSql = """
                    INSERT INTO alert_info (alert_id, transaction_id, rule_id, scenario_id)
                    VALUES (:alertId, :txnId, :ruleId, :scenarioId)
                    ON CONFLICT (transaction_id, rule_id, scenario_id) DO NOTHING 
                """;

        for (int i = 0; i < adminDays; i++) {

            LocalDate slidingAnchorDate = LocalDate.now().minusDays(i);
            flatSqlParams.put("ANCHOR_DATE", slidingAnchorDate);

            List<ExtractedCriminal> criminals = jdbcTemplate.query(finalSqlQuery, flatSqlParams, (rs, rowNum) ->
                    new ExtractedCriminal(
                            UUID.fromString(rs.getString("customer_id")),
                            (UUID[]) rs.getArray("involved_txns").getArray()
                    )
            );

            for (ExtractedCriminal criminal : criminals) {
                if (criminal.involvedTxns() == null || criminal.involvedTxns().length == 0) {
                    continue;
                }

                List<UUID> allInvolvedTxns = Arrays.asList(criminal.involvedTxns());

                Set<UUID> alreadyFlaggedTxns = alertInfoRepo.findAlreadyFlaggedTransactions(
                        blueprint.getScenarioId(),
                        allInvolvedTxns
                );

                List<UUID> netNewEvidence = new ArrayList<>(allInvolvedTxns);
                netNewEvidence.removeAll(alreadyFlaggedTxns);

                if (netNewEvidence.isEmpty()) {
                    continue;
                }

                Alert alert = new Alert();
                alert.setAlertStatus(AlertStatus.OPEN);
                alert.setJob(jobRef);
                alert.setScenario(scenarioRef);
                alert.setCustomer(customerRepo.getReferenceById(criminal.customerId()));

                Alert savedAlert = alertRepo.saveAndFlush(alert);

                List<MapSqlParameterSource> batchArgs = new ArrayList<>();

                // Fetch the rule ID for the first rule in the scenario to satisfy the NOT NULL constraint in alert_info
                UUID ruleId = jdbcTemplate.queryForObject(
                        "SELECT rule_id FROM public.rules WHERE rule_code = :code",
                        Map.of("code", blueprint.getRules().get(0).getRuleCode()),
                        UUID.class
                );

                for (UUID newTxnId : netNewEvidence) {
                    MapSqlParameterSource args = new MapSqlParameterSource()
                            .addValue("alertId", savedAlert.getAlertId())
                            .addValue("txnId", newTxnId)
                            .addValue("ruleId", ruleId)
                            .addValue("scenarioId", blueprint.getScenarioId());
                    batchArgs.add(args);
                }

                int BATCH_SIZE = 500;
                for (int j = 0; j < batchArgs.size(); j += BATCH_SIZE) {
                    List<MapSqlParameterSource> chunk = batchArgs.subList(j, Math.min(j + BATCH_SIZE, batchArgs.size()));
                    jdbcTemplate.batchUpdate(insertEvidenceSql, chunk.toArray(new MapSqlParameterSource[0]));
                }
            }
        }
    }
}