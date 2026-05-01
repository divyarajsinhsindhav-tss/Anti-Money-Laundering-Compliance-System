package org.tss.tm.ruleEngine.S1_passThrough;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tss.tm.ruleEngine.AmlRule;
import org.tss.tm.ruleEngine.AmlScenarioBlueprint;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PassThroughScenarioBlueprint implements AmlScenarioBlueprint {


    private UUID scenarioId;
    private final boolean isAggregate=true;
    private static final String scenarioCode = "S1_PASS_THROUGH";
    private final String logicalOperator=" AND ";

    @Override
    public String getScenarioCode() {
        return scenarioCode;
    }

    @Override
    public UUID getScenarioId() {
        return this.scenarioId;
    }

    @Override
    public String getLogicalOperator() {
        return logicalOperator;
    }

    @Override
    public List<AmlRule> getRules() {
        return List.of(
                new CreditToIncomeRatioRule(),
                new DebitToCreditRatioRule(),
                new LowNetRetentionRule()
        );
    }

    @Override
    public boolean isAggregateScenario() {
        return isAggregate;
    }

    @Override
    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    @Override
    public String getBaseSqlTemplate() {
        return """
            WITH customer_metrics AS (
                SELECT 
                    a.customer_id,
                    SUM(CASE WHEN ft.direction = 'IN' THEN ft.amount ELSE 0 END) AS total_credits,
                    SUM(CASE WHEN ft.direction = 'OUT' THEN ft.amount ELSE 0 END) AS total_debits,
                    ARRAY_AGG(ft.transaction_id) AS involved_txns,
                    c.income AS customerIncome
                    
                FROM financial_transaction ft
                JOIN account a ON ft.account_id = a.account_id
                JOIN customer c ON a.customer_id = c.customer_id
                WHERE ft.txn_timestamp >= CAST(:ANCHOR_DATE AS TIMESTAMP) - CAST(:LOOKBACK_DAYS || ' days' AS INTERVAL)
                  AND ft.txn_timestamp <= CAST(:ANCHOR_DATE AS TIMESTAMP)
                GROUP BY a.customer_id, c.income
            )
            SELECT customer_id, involved_txns
            FROM customer_metrics
            WHERE %s
        """;
    }
}