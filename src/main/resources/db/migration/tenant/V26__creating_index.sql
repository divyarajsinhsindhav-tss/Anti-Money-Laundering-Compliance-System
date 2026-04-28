
CREATE INDEX IF NOT EXISTS idx_staging_customers_job_id ON staging_customers(job_id);

CREATE INDEX IF NOT EXISTS idx_staging_transactions_job_id ON staging_transactions(job_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_cif ON customer(cif);
CREATE UNIQUE INDEX IF NOT EXISTS idx_account_account_number ON account(account_number);