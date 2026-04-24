ALTER TABLE account ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE financial_transaction ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE financial_transaction ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_account_updated_at') THEN
        CREATE TRIGGER trg_account_updated_at
            BEFORE UPDATE ON account
            FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_financial_transaction_updated_at') THEN
        CREATE TRIGGER trg_financial_transaction_updated_at
            BEFORE UPDATE ON financial_transaction
            FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();
    END IF;
END $$;
