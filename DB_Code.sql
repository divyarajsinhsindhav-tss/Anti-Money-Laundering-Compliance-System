select * from financial_transaction;

select * from system_admin;

select * from tenant;

select * from job_execution;

select * from customer;
select * from account;
select * from staging_customers;
select * from customer_errors;
--------------------------------TESTING-------------------------
INSERT INTO public.system_admin (
    system_admin_id,
    system_admin_code,
    first_name,
    middle_name,
    last_name,
    phone_number,
    email,
    password_hash,
    is_active,
    is_deleted,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'SYS_ADMIN_001',
    'Smit',
    'V',
    'Patel',
    '9876543210',
    'admin@example.com',
    'hashed_password',
    true,
    false,
    NOW(),
    NOW()
);

INSERT INTO public.tenant (
    tenant_id,
    tenant_code,
    name,
    display_name,
    schema_name,
    status,
    onboarded_by_admin_id,
    is_deleted,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'TENANT_001',
    'Test Tenant',
    'Test Tenant Display',
    'tenant_001_schema',
    'ONBOARDING',
    (SELECT system_admin_id FROM public.system_admin LIMIT 1),
    false,
    NOW(),     
    NOW()   
);

---------------------------------------------------------------------------

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

select * from staging_transactions;

-- CREATE TABLE temp_staging (
--     staging_id BIGSERIAL PRIMARY KEY,
--     txn_no TEXT,
--     account_number TEXT,
--     amount TEXT,
--     txn_type TEXT,
--     direction TEXT,
--     counterparty_account_no TEXT,
--     counterparty_bank_ifsc TEXT,
--     swift_code TEXT,
--     txn_timestamp TEXT,
--     country_code TEXT
-- );

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

---------------------------------------TRANSACTION PROCEDURE----------------------------------------

CREATE OR REPLACE PROCEDURE validate_transactions(p_job_id TEXT, p_tenant_id UUID)
LANGUAGE plpgsql
AS $$
BEGIN

    WITH evaluated_data AS (
        SELECT
            s.job_id,
            s.staging_id,
            s.txn_no,
            row_to_json(s)::text AS raw_row,
            
            a.account_id AS internal_account_id, 
            
            s.account_number,
            s.amount,
            s.txn_type,
            s.direction,
            s.counterparty_account_no,
            s.counterparty_bank_ifsc,
            s.txn_timestamp,
            s.country_code,
            
            ARRAY_REMOVE(ARRAY[
                CASE WHEN a.account_id IS NULL THEN 'ORPHAN_ACCOUNT_NOT_FOUND' END
            ], NULL) AS critical_array,

            ARRAY_REMOVE(ARRAY[
                CASE WHEN s.txn_type NOT IN ('DEBIT','CREDIT','REVERSAL') THEN 'INVALID_TXN_TYPE' END,
                CASE WHEN s.direction NOT IN ('IN','OUT') THEN 'INVALID_DIRECTION' END,
                CASE WHEN s.country_code IS NULL OR LENGTH(s.country_code) <> 2 THEN 'INVALID_COUNTRY_CODE' END,
                CASE WHEN pg_input_is_valid(s.amount, 'numeric') = false THEN 'INVALID_AMOUNT' END,
                CASE WHEN pg_input_is_valid(s.account_number, 'bigint') = false THEN 'INVALID_ACCOUNT_NUMBER' END,
                CASE WHEN pg_input_is_valid(s.txn_timestamp, 'timestamp') = false THEN 'INVALID_TIMESTAMP' END
            ], NULL) AS warning_array
            
        FROM staging_transactions s
        LEFT JOIN account a 
            ON a.account_number = s.account_number
            AND a.tenant_id = p_tenant_id
        WHERE s.job_id = p_job_id
    ),
    insert_errors AS (
        INSERT INTO transaction_errors (job_id, staging_id, txn_no, raw_row, critical_errors, warning_errors)
        SELECT job_id, staging_id, txn_no, raw_row, critical_array, warning_array
        FROM evaluated_data
		
        WHERE cardinality(critical_array) > 0 OR cardinality(warning_array) > 0
    )
    INSERT INTO financial_transaction (
        tenant_id, batch_id, txn_no, account_id, amount,
        txn_type, direction, counterparty_account_no,
        counterparty_bank_ifsc, txn_timestamp, country_code
    )
    SELECT
        p_tenant_id,
        p_job_id::UUID, 
        txn_no, 
        internal_account_id, 
        amount::NUMERIC(20,4), 
        txn_type, 
        direction, 
        counterparty_account_no, 
        counterparty_bank_ifsc, 
        txn_timestamp::TIMESTAMP, 
        country_code
    FROM evaluated_data
	
    WHERE (cardinality(critical_array) = 0 OR critical_array IS NULL)
      AND (cardinality(warning_array) = 0 OR warning_array IS NULL)

	ON CONFLICT (tenant_id, txn_no) 
    DO UPDATE SET 
        batch_id = EXCLUDED.batch_id,
        account_id = EXCLUDED.account_id,
        amount = EXCLUDED.amount,
        txn_type = EXCLUDED.txn_type,
        direction = EXCLUDED.direction,
        counterparty_account_no = EXCLUDED.counterparty_account_no,
        counterparty_bank_ifsc = EXCLUDED.counterparty_bank_ifsc,
        txn_timestamp = EXCLUDED.txn_timestamp,
        country_code = EXCLUDED.country_code;

    DELETE FROM staging_transactions
    WHERE job_id = p_job_id;

END;
$$;
-----------------------------------------------------------------------------------------------------


ALTER TABLE financial_transaction 
ADD CONSTRAINT uk_tenant_txn_no UNIQUE (tenant_id, txn_no);

------------------------------------------CUSTOMER TABLE--------------------------------------------
CREATE OR REPLACE PROCEDURE validate_customers(p_job_id TEXT, p_tenant_id UUID)
LANGUAGE plpgsql
AS $$
BEGIN

    WITH evaluated_data AS (
        SELECT
            s.job_id, s.staging_id, s.cif, s.first_name, s.middle_name, s.last_name, 
            s.dob, s.income, s.account_number, s.account_type, s.opened_at,
            row_to_json(s)::text AS raw_row,
            
            ARRAY_REMOVE(ARRAY[
                CASE WHEN s.cif IS NULL OR TRIM(s.cif) = '' THEN 'MISSING_CIF' END,
                CASE WHEN s.account_number IS NULL OR TRIM(s.account_number) = '' THEN 'MISSING_ACCOUNT_NUMBER' END,
				CASE WHEN NOT pg_input_is_valid(s.account_number, 'bigint') THEN 'INVALID_ACCOUNT_NUMBER_FORMAT' END,
                CASE WHEN s.first_name IS NULL OR TRIM(s.first_name) = '' 
                       OR s.last_name IS NULL OR TRIM(s.last_name) = '' THEN 'MISSING_FIRST_OR_LAST_NAME' END,
                CASE WHEN s.income IS NULL OR TRIM(s.income) = '' THEN 'MISSING_INCOME' END
				-- CASE WHEN s.account_type IS NULL OR s.account_type NOT IN ('SAVINGS','CURRENT') THEN 'INVALID_ACCOUNT_TYPE' END
            ], NULL) AS critical_array,

            ARRAY_REMOVE(ARRAY[
                CASE WHEN s.dob IS NOT NULL AND TRIM(s.dob) <> '' AND pg_input_is_valid(s.dob, 'date') = false THEN 'INVALID_DOB_FORMAT' END,
                CASE WHEN NOT pg_input_is_valid(s.income, 'numeric') THEN 'INVALID_INCOME' END,
				CASE WHEN pg_input_is_valid(s.income, 'numeric') AND (s.income::numeric < 0) THEN 'NEGATIVE_INCOME' END,
                CASE WHEN s.opened_at IS NOT NULL AND TRIM(s.opened_at) <> '' AND pg_input_is_valid(s.opened_at, 'timestamp') = false THEN 'INVALID_OPENED_AT_FORMAT' END,
				CASE WHEN pg_input_is_valid(s.dob, 'date') AND s.dob::date > CURRENT_DATE THEN 'FUTURE_DOB' END,
				CASE WHEN pg_input_is_valid(s.opened_at, 'timestamp') AND s.opened_at::timestamp > CURRENT_TIMESTAMP THEN 'FUTURE_OPENED_AT' END
            ], NULL) AS warning_array
            
        FROM staging_customers s
        WHERE s.job_id = p_job_id
    ),
    
    insert_errors AS (
        INSERT INTO customer_errors (job_id, cif, staging_id, raw_row, critical_errors, warning_errors)
        SELECT job_id, cif, staging_id, raw_row, critical_array, warning_array
        FROM evaluated_data
        WHERE cardinality(critical_array) > 0 OR cardinality(warning_array) > 0
    ),
    
    deduped_customers AS (
        SELECT 
            cif, 
            MAX(first_name) AS first_name, 
            MAX(middle_name) AS middle_name, 
            MAX(last_name) AS last_name, 
            MAX(dob) AS dob, 
            MAX(income) AS income
        FROM evaluated_data
        WHERE (cardinality(critical_array) = 0 OR critical_array IS NULL)
          AND (cardinality(warning_array) = 0 OR warning_array IS NULL)
        GROUP BY cif
    ),
    
   upsert_customers AS (
        INSERT INTO customer (
            tenant_id, cif, first_name, middle_name, last_name, dob, income, 
            created_at, updated_at, is_deleted
        )
        SELECT 
            p_tenant_id, cif, first_name, middle_name, last_name, 
            dob::DATE, income::NUMERIC(18,2),
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
        FROM deduped_customers
        
        ON CONFLICT (tenant_id, cif) 
        DO UPDATE SET 
            first_name = EXCLUDED.first_name,
            middle_name = EXCLUDED.middle_name,
            last_name = EXCLUDED.last_name,
            dob = EXCLUDED.dob,
            income = EXCLUDED.income,
            updated_at = CURRENT_TIMESTAMP
            
        RETURNING cif, customer_id
    )
    
    INSERT INTO account (
        tenant_id, customer_id, account_number, account_type, opened_at,
        created_at, updated_at, is_deleted
    )
    SELECT 
        p_tenant_id, u.customer_id, e.account_number, e.account_type, e.opened_at::TIMESTAMP,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false 
    FROM evaluated_data e
    INNER JOIN upsert_customers u ON u.cif = e.cif 
    WHERE (cardinality(e.critical_array) = 0 OR e.critical_array IS NULL)
      AND (cardinality(e.warning_array) = 0 OR e.warning_array IS NULL)
      
    ON CONFLICT (tenant_id, account_number) 
    DO UPDATE SET 
        customer_id = EXCLUDED.customer_id,
        account_type = EXCLUDED.account_type,
        opened_at = EXCLUDED.opened_at,
        updated_at = CURRENT_TIMESTAMP;
		
    DELETE FROM staging_customers
    WHERE job_id = p_job_id;

END;
$$;