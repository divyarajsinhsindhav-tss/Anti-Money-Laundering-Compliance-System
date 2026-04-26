package org.tss.tm.ruleEngine;


import java.util.List;
import java.util.UUID;

public class PassThroughScenarioBlueprint implements AmlScenarioBlueprint {

    private final UUID scenarioId;

    public PassThroughScenarioBlueprint(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    @Override
    public UUID getScenarioId() {
        return this.scenarioId;
    }

    @Override
    public String getLogicalOperator() {
        return " AND ";
    }

    @Override
    public List<AmlRule> getRules() {
        return List.of(
                new HighCreditTurnoverRule(),
                new HighDebitTurnoverRule(),
                new LowNetRetentionRule()
        );
    }

    @Override
    public String getBaseSqlTemplate() {
        return """
            WITH customer_metrics AS (
                SELECT 
                    customer_id,
                    SUM(CASE WHEN txn_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits,
                    SUM(CASE WHEN txn_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debits,
                    ARRAY_AGG(transaction_id) AS involved_txns
                FROM financial_transaction
                WHERE txn_timestamp >= CAST(:ANCHOR_DATE AS TIMESTAMP) - CAST(:LOOKBACK_DAYS || ' days' AS INTERVAL)
                  AND txn_timestamp <= CAST(:ANCHOR_DATE AS TIMESTAMP)
                GROUP BY customer_id
            )
            SELECT customer_id, involved_txns
            FROM customer_metrics
            WHERE %s
        """;
    }
}