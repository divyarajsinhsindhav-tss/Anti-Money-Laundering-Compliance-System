ALTER TABLE customer_errors DROP COLUMN IF EXISTS staging_id;
ALTER TABLE transaction_errors DROP COLUMN IF EXISTS staging_id;
