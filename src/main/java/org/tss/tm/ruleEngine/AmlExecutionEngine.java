package org.tss.tm.ruleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.repository.*;
import org.tss.tm.service.interfaces.ScenarioParamService;

import java.util.*;
import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Collectors;

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
    private final RuleRepo ruleRepo;
    private final TransactionTemplate transactionTemplate;

    private record ExtractedCriminal(UUID customerId, UUID[] involvedTxns) {}

    public void executeMultipleTxnScenario(AmlScenarioBlueprint blueprint, LocalDate anchorDate, UUID currentJobId, int effectiveLookback) {

        if (blueprint.getRules().isEmpty()) return;

        Map<String, Map<String, Object>> nestedParams = paramService.getParams(blueprint.getScenarioId());
        Map<String, Object> flatSqlParams = new HashMap<>();

        flatSqlParams.put("LOOKBACK_DAYS", effectiveLookback);
        flatSqlParams.put("ANCHOR_DATE", anchorDate);

        for (Map.Entry<String, Map<String, Object>> ruleEntry : nestedParams.entrySet()) {
            String ruleCode = ruleEntry.getKey();
            for (Map.Entry<String, Object> paramEntry : ruleEntry.getValue().entrySet()) {
                if (!paramEntry.getKey().equals("LOOKBACK_DAYS")) {
                    flatSqlParams.put(ruleCode + "_" + paramEntry.getKey(), paramEntry.getValue());
                }
            }
        }

        List<String> compiledConditions = new ArrayList<>();
        List<String> ruleCodes = new ArrayList<>();

        for (AmlRule rule : blueprint.getRules()) {
            Map<String, Object> ruleParams = nestedParams.getOrDefault(rule.getRuleCode(), Collections.emptyMap());

            if (!ruleParams.keySet().stream().filter(k -> !k.equals("LOOKBACK_DAYS"))
                    .allMatch(k -> flatSqlParams.containsKey(rule.getRuleCode() + "_" + k))) {
                throw new IllegalStateException("Parameter mismatch in rule: " + rule.getRuleCode());
            }

            compiledConditions.add(rule.getBooleanCondition(ruleParams));
            ruleCodes.add(rule.getRuleCode());
        }

        Map<String, Rule> dbRuleMap = ruleRepo.findAllByRuleCodeIn(ruleCodes).stream()
                .collect(Collectors.toMap(Rule::getRuleCode, r -> r));

        List<Rule> dbRulesForScenario = new ArrayList<>();
        for (String code : ruleCodes) {
            Rule dbRule = dbRuleMap.get(code);
            if (dbRule == null) throw new IllegalStateException("Missing DB rule: " + code);
            dbRulesForScenario.add(dbRule);
        }

        String dynamicWhereClause = String.join(blueprint.getLogicalOperator(), compiledConditions);
        String finalSqlQuery = String.format(blueprint.getBaseSqlTemplate(), dynamicWhereClause);

        Scenario scenarioRef = scenarioRepo.getReferenceById(blueprint.getScenarioId());
        JobRecord jobRef = jobRepo.getReferenceById(currentJobId);

        String insertRuleSql = """
                INSERT INTO alert_info (alert_id, rule_id, scenario_id)
                VALUES (:alertId, :ruleId, :scenarioId)
                ON CONFLICT (alert_id, rule_id) WHERE transaction_id IS NULL DO NOTHING
            """;

        String insertTxnSql = """
                INSERT INTO alert_info (alert_id, transaction_id, scenario_id)
                VALUES (:alertId, :txnId, :scenarioId)
                ON CONFLICT (alert_id, transaction_id) WHERE rule_id IS NULL DO NOTHING
            """;

        log.info("Executing AML Engine query for scenario {} at anchor {} with effective lookback {}",
                blueprint.getScenarioId(), anchorDate, effectiveLookback);

        List<ExtractedCriminal> criminals = jdbcTemplate.query(finalSqlQuery, flatSqlParams, (rs, rowNum) -> {
            UUID customerId = UUID.fromString(rs.getString("customer_id"));

            java.sql.Array sqlArray = rs.getArray("involved_txns");
            if (sqlArray == null) {
                log.warn("SQL Data Issue: Customer {} returned with NULL involved_txns array.", customerId);
                return new ExtractedCriminal(customerId, new UUID[0]);
            }

            Object[] rawArray = (Object[]) sqlArray.getArray();
            if (rawArray == null || rawArray.length == 0) {
                log.warn("SQL Data Issue: Customer {} returned with EMPTY involved_txns array.", customerId);
                return new ExtractedCriminal(customerId, new UUID[0]);
            }

            UUID[] txns = Arrays.stream(rawArray)
                    .map(obj -> UUID.fromString(obj.toString()))
                    .toArray(UUID[]::new);

            return new ExtractedCriminal(customerId, txns);
        });

        for (ExtractedCriminal criminal : criminals) {
            if (criminal.involvedTxns().length == 0) continue;

            List<UUID> allInvolvedTxns = Arrays.asList(criminal.involvedTxns());

            Set<UUID> alreadyFlaggedTxns = alertInfoRepo.findAlreadyFlaggedTransactions(
                    blueprint.getScenarioId(), allInvolvedTxns);

            List<UUID> netNewEvidence = new ArrayList<>(allInvolvedTxns);
            netNewEvidence.removeAll(alreadyFlaggedTxns);

            if (netNewEvidence.isEmpty()) continue;

            transactionTemplate.execute(status -> {
                try {
                    Optional<Alert> existingAlert=alertRepo.findByCustomer_CustomerIdAndScenario_ScenarioIdAndAlertStatus(
                            criminal.customerId(), blueprint.getScenarioId(), AlertStatus.OPEN
                    );
                    Alert targetAlert;

                    if(existingAlert.isPresent()){
                        targetAlert=existingAlert.get();
                        log.info("Appending new evidence to existing OPEN alert {} for customer {}",
                                targetAlert.getAlertId(), criminal.customerId());
                    }else{
                        Alert alert = new Alert();
                        alert.setAlertStatus(AlertStatus.OPEN);
                        alert.setJob(jobRef);
                        alert.setScenario(scenarioRef);
                        alert.setCustomer(customerRepo.getReferenceById(criminal.customerId()));
                        targetAlert = alertRepo.saveAndFlush(alert);
                    }


                    List<MapSqlParameterSource> ruleBatchArgs = new ArrayList<>();
                    for (Rule dbRule : dbRulesForScenario) {
                        MapSqlParameterSource ruleArgs = new MapSqlParameterSource()
                                .addValue("alertId", targetAlert.getAlertId())
                                .addValue("ruleId", dbRule.getRuleId())
                                .addValue("scenarioId", blueprint.getScenarioId());
                        ruleBatchArgs.add(ruleArgs);
                    }
                    jdbcTemplate.batchUpdate(insertRuleSql, ruleBatchArgs.toArray(new MapSqlParameterSource[0]));

                    List<MapSqlParameterSource> txnBatchArgs = new ArrayList<>();
                    for (UUID newTxnId : netNewEvidence) {
                        MapSqlParameterSource txnArgs = new MapSqlParameterSource()
                                .addValue("alertId", targetAlert.getAlertId())
                                .addValue("txnId", newTxnId)
                                .addValue("scenarioId", blueprint.getScenarioId());
                        txnBatchArgs.add(txnArgs);
                    }

                    int BATCH_SIZE = 500;
                    for (int j = 0; j < txnBatchArgs.size(); j += BATCH_SIZE) {
                        List<MapSqlParameterSource> chunk = txnBatchArgs.subList(j, Math.min(j + BATCH_SIZE, txnBatchArgs.size()));
                        jdbcTemplate.batchUpdate(insertTxnSql, chunk.toArray(new MapSqlParameterSource[0]));
                    }
                } catch (DataIntegrityViolationException e) {
                    log.info("Concurrency Guard: Open alert already exists for customer {} under scenario {}. Skipping.",
                            criminal.customerId(), blueprint.getScenarioId());
                    status.setRollbackOnly();
                } catch (Exception e) {
                    log.error("Failed to save alert for customer {}", criminal.customerId(), e);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    public void executeSingleTxnScenario(AmlScenarioBlueprint blueprint, LocalDate anchorDate, UUID currentJobId) {
        if (blueprint.getRules().isEmpty()) return;

        Map<String, Map<String, Object>> nestedParams = paramService.getParams(blueprint.getScenarioId());
        Map<String, Object> flatSqlParams = new HashMap<>();
        flatSqlParams.put("ANCHOR_DATE", anchorDate);

        List<String> ruleCodes = new ArrayList<>();
        for (AmlRule rule : blueprint.getRules()) {
            ruleCodes.add(rule.getRuleCode());
            Map<String, Object> ruleParams = nestedParams.getOrDefault(rule.getRuleCode(), Collections.emptyMap());
            for (Map.Entry<String, Object> param : ruleParams.entrySet()) {
                flatSqlParams.put(rule.getRuleCode() + "_" + param.getKey(), param.getValue());
            }
        }

        Map<String, Rule> dbRuleMap = ruleRepo.findAllByRuleCodeIn(ruleCodes).stream()
                .collect(Collectors.toMap(Rule::getRuleCode, r -> r));

        Scenario scenarioRef = scenarioRepo.getReferenceById(blueprint.getScenarioId());
        JobRecord jobRef = jobRepo.getReferenceById(currentJobId);

        String finalSqlQuery = blueprint.getBaseSqlTemplate();

        String insert1to1Sql = """
            INSERT INTO alert_info (alert_id, rule_id, scenario_id, transaction_id)
            VALUES (:alertId, :ruleId, :scenarioId, :txnId)
            ON CONFLICT (scenario_id, rule_id, transaction_id) DO NOTHING
        """;

        jdbcTemplate.query(finalSqlQuery, flatSqlParams, rs -> {
            UUID customerId = UUID.fromString(rs.getString("customer_id"));
            String txnStr = rs.getString("transaction_id");
            if (txnStr == null) {
                log.warn("Missing transaction_id for customer {}", customerId);
                return;
            }
            UUID txnId = UUID.fromString(txnStr);

            String brokenRuleCode = rs.getString("rule_code");
            if (brokenRuleCode == null) return;

            Rule dbRule = dbRuleMap.get(brokenRuleCode);
            if (dbRule == null) {
                log.error("Rule mapping missing for rule_code {}", brokenRuleCode);
                return;
            }
            UUID brokenRuleId = dbRule.getRuleId();

            transactionTemplate.execute(status -> {
                try {
                    Alert targetAlert = alertRepo.findByCustomer_CustomerIdAndScenario_ScenarioIdAndAlertStatus(
                                    customerId, blueprint.getScenarioId(), AlertStatus.OPEN)
                            .orElseGet(() -> {
                                Alert newAlert = new Alert();
                                newAlert.setAlertStatus(AlertStatus.OPEN);
                                newAlert.setJob(jobRef);
                                newAlert.setScenario(scenarioRef);
                                newAlert.setCustomer(customerRepo.getReferenceById(customerId));
                                return alertRepo.saveAndFlush(newAlert);
                            });

                    MapSqlParameterSource args = new MapSqlParameterSource()
                            .addValue("alertId", targetAlert.getAlertId())
                            .addValue("ruleId", brokenRuleId)
                            .addValue("txnId", txnId)
                            .addValue("scenarioId", blueprint.getScenarioId());

                    jdbcTemplate.update(insert1to1Sql, args);

                } catch (DataIntegrityViolationException e) {
                    log.info("Concurrency Guard skipped txn {}", txnId);
                    status.setRollbackOnly();
                }
                return null;
            });
        });
    }
}