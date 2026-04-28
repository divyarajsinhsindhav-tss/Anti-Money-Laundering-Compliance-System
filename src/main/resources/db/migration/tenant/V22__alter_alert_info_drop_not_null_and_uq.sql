-- Drop all foreign key constraints first to avoid dependency issues
ALTER TABLE alert_info DROP CONSTRAINT IF EXISTS fk_alert_info_transaction;
ALTER TABLE alert_info DROP CONSTRAINT IF EXISTS fk_alert_info_rule;

-- Make transaction_id and rule_id nullable as per recent entity changes
ALTER TABLE alert_info ALTER COLUMN transaction_id DROP NOT NULL;
ALTER TABLE alert_info ALTER COLUMN rule_id DROP NOT NULL;

-- Re-add foreign key constraints with ON DELETE SET NULL
ALTER TABLE alert_info ADD CONSTRAINT fk_alert_info_transaction FOREIGN KEY (transaction_id) REFERENCES financial_transaction(transaction_id) ON DELETE SET NULL;
ALTER TABLE alert_info ADD CONSTRAINT fk_alert_info_rule FOREIGN KEY (rule_id) REFERENCES rule(rule_id) ON DELETE SET NULL;
