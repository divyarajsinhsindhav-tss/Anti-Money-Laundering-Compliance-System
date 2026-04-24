CREATE TABLE staging_customers (
   staging_id BIGSERIAL PRIMARY KEY,
   job_id TEXT NOT NULL,

   cif TEXT,
   first_name TEXT,
   middle_name TEXT,
   last_name TEXT,
   dob TEXT,
   income TEXT,
   account_number TEXT,
   account_type TEXT,
   opened_at TEXT
);

CREATE TABLE customer_errors (
     error_id BIGSERIAL PRIMARY KEY,

     job_id TEXT NOT NULL,
     cif TEXT,
     staging_id BIGINT,

     raw_row TEXT,

     critical_errors TEXT[],
     warning_errors TEXT[],

     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE customer ADD CONSTRAINT uk_tenant_cif UNIQUE (tenant_id, cif);
ALTER TABLE account ADD CONSTRAINT uk_tenant_account_no UNIQUE (tenant_id, account_number);

CREATE TABLE staging_transactions (
    staging_id BIGSERIAL PRIMARY KEY,
    job_id TEXT NOT NULL,

    txn_no TEXT,
    account_number TEXT,
    amount TEXT,
    txn_type TEXT,
    direction TEXT,
    counterparty_account_no TEXT,
    counterparty_bank_ifsc TEXT,
    swift_code TEXT,
    txn_timestamp TEXT,
    country_code TEXT
);


CREATE TABLE transaction_errors (
    error_id BIGSERIAL PRIMARY KEY,
    job_id TEXT NOT NULL,
    txn_no TEXT,
    staging_id BIGINT,
    raw_row TEXT,
    critical_errors TEXT[],
    warning_errors TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE financial_transaction
ADD CONSTRAINT uk_tenant_txn_no UNIQUE (tenant_id, txn_no);