-- Drop existing indices if they exist to avoid conflicts
DROP INDEX IF EXISTS idx_alert_rule_unique;
DROP INDEX IF EXISTS idx_alert_txn_unique;
DROP INDEX IF EXISTS idx_alert_rule_txn_unique;
DROP INDEX IF EXISTS idx_unique_1to1;

CREATE UNIQUE INDEX idx_alert_rule_unique
    ON alert_info (alert_id, rule_id)
    WHERE transaction_id IS NULL;

CREATE UNIQUE INDEX idx_alert_txn_unique
    ON alert_info (alert_id, transaction_id)
    WHERE rule_id IS NULL;

CREATE UNIQUE INDEX idx_unique_1to1
    ON alert_info (scenario_id, rule_id, transaction_id)
    WHERE rule_id IS NOT NULL AND transaction_id IS NOT NULL;