-- Update account table constraint
ALTER TABLE account DROP CONSTRAINT IF EXISTS account_account_number_check;
ALTER TABLE account ADD CONSTRAINT account_account_number_check CHECK (account_number ~ '^[0-9]{6,30}$');

-- Update financial_transaction table constraints
ALTER TABLE financial_transaction DROP CONSTRAINT IF EXISTS financial_transaction_counterparty_account_no_check;
ALTER TABLE financial_transaction ADD CONSTRAINT financial_transaction_counterparty_account_no_check CHECK (counterparty_account_no IS NULL OR counterparty_account_no ~ '^[0-9]{4,30}$');