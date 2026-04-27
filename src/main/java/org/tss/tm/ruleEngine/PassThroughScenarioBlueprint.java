package org.tss.tm.ruleEngine;


import java.util.List;
import java.util.UUID;

public class PassThroughScenarioBlueprint implements AmlScenarioBlueprint {

    private final UUID scenarioId;
    private final boolean isAggregate=true;

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
    public boolean isAggregateScenario() {
        return isAggregate;
    }

    @Override
    public String getBaseSqlTemplate() {
        return """
            WITH customer_metrics AS (
                SELECT 
                    a.customer_id,
                    SUM(CASE WHEN ft.direction = 'IN' THEN ft.amount ELSE 0 END) AS total_credits,
                    SUM(CASE WHEN ft.direction = 'OUT' THEN ft.amount ELSE 0 END) AS total_debits,
                    ARRAY_AGG(ft.transaction_id) AS involved_txns
                FROM financial_transaction ft
                JOIN account a ON ft.account_id = a.account_id
                WHERE ft.txn_timestamp >= CAST(:ANCHOR_DATE AS TIMESTAMP) - CAST(:LOOKBACK_DAYS || ' days' AS INTERVAL)
                  AND ft.txn_timestamp <= CAST(:ANCHOR_DATE AS TIMESTAMP)
                GROUP BY a.customer_id
            )
            SELECT customer_id, involved_txns
            FROM customer_metrics
            WHERE %s
        """;
    }
}