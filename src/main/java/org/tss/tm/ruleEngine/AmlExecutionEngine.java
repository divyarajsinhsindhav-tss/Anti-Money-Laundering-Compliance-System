package org.tss.tm.ruleEngine;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.common.enums.ErrorSeverity;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Rule;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.RuleEngineError;
import org.tss.tm.repository.*;
import org.tss.tm.service.interfaces.ParamService;

import java.util.*;
import java.time.LocalDate;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlExecutionEngine {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ParamService paramService;
    private final AlertRepo alertRepo;
    private final AlertInfoRepo alertInfoRepo;
    private final ScenarioRepo scenarioRepo;
    private final CustomerRepo customerRepo;
    private final JobRepo jobRepo;
    private final RuleRepo ruleRepo;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final RuleEngineErrorRepo ruleEngineErrorRepo;

    private record ExtractedCriminal(UUID customerId, UUID[] involvedTxns) {
    }

    public void executeMultipleTxnScenario(AmlScenarioBlueprint blueprint, Map<String, Map<String, Object>> nestedParams, LocalDate toDate, UUID currentJobId, long lookbackDays) {

        if (blueprint.getRules().isEmpty())
            return;

        Map<String, Object> flatSqlParams = new HashMap<>();
        flatSqlParams.put("LOOKBACK_DAYS", lookbackDays);

        flatSqlParams.put("ANCHOR_DATE", toDate);

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
                RuleEngineError ruleEngineError = RuleEngineError.builder().info("Parameter Failure: Parameter mismatch for rules").scenarioCode(blueprint.getScenarioCode()).jobId(String.valueOf(currentJobId)).ruleCode(rule.getRuleCode()).severity(ErrorSeverity.MEDIUM).build();
                ruleEngineErrorRepo.save(ruleEngineError);
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
            if (dbRule == null) {
                RuleEngineError ruleEngineError=RuleEngineError.builder().info("Rule Failure: Internal issue").scenarioCode(blueprint.getScenarioCode()).jobId(String.valueOf(currentJobId)).ruleCode(code).severity(ErrorSeverity.MEDIUM).build();
                ruleEngineErrorRepo.save(ruleEngineError);
                throw new IllegalStateException("Missing DB rule: " + code);
            }
            dbRulesForScenario.add(dbRule);
        }

        String dynamicWhereClause = String.join(blueprint.getLogicalOperator(), compiledConditions);
        log.info("Search path: {}",
                jdbcTemplate.getJdbcTemplate()
                        .queryForObject("SHOW search_path", String.class));
        String finalSqlQuery = String.format(blueprint.getBaseSqlTemplate(), dynamicWhereClause);

        Scenario scenarioRef = scenarioRepo.getReferenceById(blueprint.getScenarioId());
        JobRecord jobRef = jobRepo.getReferenceById(currentJobId);

        String insertRuleSql = """
                    INSERT INTO alert_info (alert_id, rule_id, scenario_id)
                    VALUES (?, ?, ?)
                    ON CONFLICT (alert_id, rule_id) WHERE transaction_id IS NULL DO NOTHING
                """;

        String insertTxnSql = """
                    INSERT INTO alert_info (alert_id, transaction_id, scenario_id)
                    VALUES (?, ?, ?)
                    ON CONFLICT (alert_id, transaction_id) WHERE rule_id IS NULL DO NOTHING
                """;
        log.info("Executing AML Engine query for scenario {} at anchor {} with effective lookback {}",
                blueprint.getScenarioId(), toDate, lookbackDays);

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
            if (criminal.involvedTxns().length == 0)
                continue;

            List<UUID> allInvolvedTxns = Arrays.asList(criminal.involvedTxns());

            Set<UUID> alreadyFlaggedTxns = alertInfoRepo.findAlreadyFlaggedTransactions(
                    blueprint.getScenarioId(), allInvolvedTxns);

            List<UUID> netNewEvidence = new ArrayList<>(allInvolvedTxns);
            netNewEvidence.removeAll(alreadyFlaggedTxns);

            if (netNewEvidence.isEmpty())
                continue;

            transactionTemplate.execute(status -> {
                try {
                    Optional<Alert> existingAlert = alertRepo
                            .findByCustomer_CustomerIdAndScenario_ScenarioIdAndAlertStatus(
                                    criminal.customerId(), blueprint.getScenarioId(), AlertStatus.OPEN);
                    Alert targetAlert;

                    if (existingAlert.isPresent()) {
                        targetAlert = existingAlert.get();
                        log.info("Appending new evidence to existing OPEN alert {} for customer {}",
                                targetAlert.getAlertId(), criminal.customerId());
                    } else {
                        Alert alert = new Alert();
                        alert.setAlertStatus(AlertStatus.OPEN);
                        alert.setJob(jobRef);
                        alert.setScenario(scenarioRef);
                        alert.setCustomer(customerRepo.getReferenceById(criminal.customerId()));
                        targetAlert = alertRepo.saveAndFlush(alert);
                    }

                    Session session = entityManager.unwrap(Session.class);

                    session.doWork(connection -> {
                        try (java.sql.PreparedStatement psRule = connection.prepareStatement(insertRuleSql)) {
                            for (Rule dbRule : dbRulesForScenario) {
                                psRule.setObject(1, targetAlert.getAlertId());
                                psRule.setObject(2, dbRule.getRuleId());
                                psRule.setObject(3, blueprint.getScenarioId());
                                psRule.addBatch();
                            }
                            psRule.executeBatch();
                        }

                        try (java.sql.PreparedStatement psTxn = connection.prepareStatement(insertTxnSql)) {
                            int count = 0;
                            for (UUID newTxnId : netNewEvidence) {
                                psTxn.setObject(1, targetAlert.getAlertId());
                                psTxn.setObject(2, newTxnId);
                                psTxn.setObject(3, blueprint.getScenarioId());
                                psTxn.addBatch();

                                if (++count % 500 == 0) {
                                    psTxn.executeBatch();
                                }
                            }
                            psTxn.executeBatch();
                        }
                    });
                } catch (Exception e) {
                    log.error("AML Multiple Txn Scenario Failed. Exception: {}", e.getMessage());
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    record SingleTxnResult(UUID customerId, UUID txnId, UUID brokenRuleId) {
    }

    public void executeSingleTxnScenario(AmlScenarioBlueprint blueprint, Map<String, Map<String, Object>> nestedParams, LocalDate anchorDate, UUID currentJobId) {
        if (blueprint.getRules().isEmpty())
            return;

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
                    INSERT INTO alert_info (alert_info_id, alert_id, rule_id, scenario_id, transaction_id)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?)
                    ON CONFLICT (scenario_id, rule_id, transaction_id) 
                    WHERE rule_id IS NOT NULL AND transaction_id IS NOT NULL 
                    DO NOTHING
                """;

        List<SingleTxnResult> results = jdbcTemplate.query(finalSqlQuery, flatSqlParams, (rs, rowNum) -> {
            UUID customerId = UUID.fromString(rs.getString("customer_id"));
            String txnStr = rs.getString("transaction_id");
            if (txnStr == null) return null;

            String brokenRuleCode = rs.getString("rule_code");
            if (brokenRuleCode == null) return null;

            Rule dbRule = dbRuleMap.get(brokenRuleCode);
            if (dbRule == null) return null;

            return new SingleTxnResult(customerId, UUID.fromString(txnStr), dbRule.getRuleId());
        });

        results.removeIf(Objects::isNull);
        if (results.isEmpty()) return;

        Map<UUID, List<SingleTxnResult>> groupedByCustomer = results.stream()
                .collect(Collectors.groupingBy(SingleTxnResult::customerId));

        for (Map.Entry<UUID, List<SingleTxnResult>> entry : groupedByCustomer.entrySet()) {
            UUID customerId = entry.getKey();
            List<SingleTxnResult> customerViolations = entry.getValue();

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

                    Session session = entityManager.unwrap(org.hibernate.Session.class);
                    session.doWork(connection -> {
                        try (java.sql.PreparedStatement ps = connection.prepareStatement(insert1to1Sql)) {
                            for (SingleTxnResult violation : customerViolations) {
                                ps.setObject(1, targetAlert.getAlertId());
                                ps.setObject(2, violation.brokenRuleId());
                                ps.setObject(3, blueprint.getScenarioId());
                                ps.setObject(4, violation.txnId());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    });

                } catch (Exception e) {
                    log.error("AML Single Txn Scenario failed. Exception: {}", e.getMessage());
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }
}