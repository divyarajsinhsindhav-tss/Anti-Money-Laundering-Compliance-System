CREATE OR REPLACE PROCEDURE validate_transactions(p_job_id TEXT)
LANGUAGE plpgsql
AS $$
BEGIN

    CREATE TEMP TABLE evaluated_data ON COMMIT DROP AS
SELECT
    s.job_id,
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
                     CASE WHEN s.account_number IS NULL OR s.account_number = '' THEN 'MISSING_ACCOUNT_NUMBER' END,
                 CASE WHEN s.account_number !~ '^[0-9]{6,30}$' THEN 'INVALID_ACCOUNT_NUMBER' END,
                 CASE WHEN a.account_id IS NULL THEN 'ACCOUNT_NOT_FOUND' END
                     ], NULL) AS critical_error_array,

    ARRAY_REMOVE(ARRAY[
                     CASE WHEN UPPER(TRIM(s.txn_type)) NOT IN ('NEFT', 'RTGS', 'IMPS', 'UPI', 'CASH', 'CHEQUE', 'INTERNAL', 'WIRE') THEN 'INVALID_TXN_TYPE' END,
                 CASE WHEN UPPER(TRIM(s.direction)) NOT IN ('IN','OUT') THEN 'INVALID_DIRECTION' END,
                 CASE WHEN s.country_code IS NULL OR LENGTH(TRIM(s.country_code)) <> 2 THEN 'INVALID_COUNTRY_CODE' END,
                 CASE WHEN s.amount !~ '^[0-9]+(\.[0-9]+)?$' THEN 'INVALID_AMOUNT' END,
                 CASE WHEN s.txn_timestamp IS NULL OR s.txn_timestamp = '' OR pg_input_is_valid(s.txn_timestamp, 'timestamp') = false THEN 'INVALID_TIMESTAMP' END
                     ], NULL) AS non_critical_error_array
FROM staging_transactions s
         LEFT JOIN account a
                   ON a.account_number = s.account_number AND a.is_deleted = false
WHERE s.job_id = p_job_id;

INSERT INTO transaction_errors (job_id, txn_no, raw_row, critical_errors, warning_errors)
SELECT job_id, txn_no, raw_row, critical_error_array, non_critical_error_array
FROM evaluated_data
WHERE cardinality(critical_error_array) > 0
   OR cardinality(non_critical_error_array) > 0;

CREATE TEMP TABLE clean_deduped_data ON COMMIT DROP AS
SELECT DISTINCT ON (txn_no) *
FROM evaluated_data
WHERE (cardinality(critical_error_array) = 0 OR critical_error_array IS NULL)
  AND (cardinality(non_critical_error_array) = 0 OR non_critical_error_array IS NULL)
ORDER BY txn_no, txn_timestamp::TIMESTAMP DESC;

INSERT INTO financial_transaction (
    batch_id, txn_no, account_id, amount,
    txn_type, direction, counterparty_account_no,
    counterparty_bank_ifsc, txn_timestamp, country_code,
    created_at, updated_at, is_deleted
)
SELECT
    p_job_id::UUID,
    txn_no,
    internal_account_id,
    amount::NUMERIC(20,4),
    UPPER(TRIM(txn_type))::txn_type_enum,
    UPPER(TRIM(direction))::direction_enum,
    counterparty_account_no,
    counterparty_bank_ifsc,
    txn_timestamp::TIMESTAMP,
    country_code,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
FROM clean_deduped_data
    ON CONFLICT (txn_no)
    DO UPDATE SET
    batch_id = EXCLUDED.batch_id,
               account_id = EXCLUDED.account_id,
               amount = EXCLUDED.amount,
               txn_type = EXCLUDED.txn_type,
               direction = EXCLUDED.direction,
               counterparty_account_no = EXCLUDED.counterparty_account_no,
               counterparty_bank_ifsc = EXCLUDED.counterparty_bank_ifsc,
               txn_timestamp = EXCLUDED.txn_timestamp,
               country_code = EXCLUDED.country_code,
               updated_at = CURRENT_TIMESTAMP;

DELETE FROM staging_transactions
WHERE job_id = p_job_id;

END;
$$;


CREATE OR REPLACE PROCEDURE validate_customers(p_job_id TEXT)
LANGUAGE plpgsql
AS $$
BEGIN

    CREATE TEMP TABLE evaluated_data ON COMMIT DROP AS
SELECT
    s.job_id, s.cif, s.first_name, s.middle_name, s.last_name,
    s.dob, s.income, s.account_number, s.account_type, s.opened_at,
    row_to_json(s)::text AS raw_row,

    ARRAY_REMOVE(ARRAY[
                     CASE WHEN s.cif IS NULL OR TRIM(s.cif) = '' THEN 'MISSING_CIF' END,
                 CASE WHEN s.account_number IS NULL OR TRIM(s.account_number) = '' THEN 'MISSING_ACCOUNT_NUMBER' END,
                 CASE WHEN s.account_number !~ '^[0-9]{6,30}$' THEN 'INVALID_ACCOUNT_NUMBER_FORMAT' END,
                 CASE WHEN s.first_name IS NULL OR TRIM(s.first_name) = ''
                     OR s.last_name IS NULL OR TRIM(s.last_name) = '' THEN 'MISSING_FIRST_OR_LAST_NAME' END,
                 CASE WHEN s.income IS NULL OR TRIM(s.income) = '' THEN 'MISSING_INCOME' END
                     ], NULL) AS critical_error_array,

    ARRAY_REMOVE(ARRAY[
                     CASE WHEN s.dob IS NOT NULL AND TRIM(s.dob) <> '' AND pg_input_is_valid(s.dob, 'date') = false THEN 'INVALID_DOB_FORMAT' END,
                 CASE WHEN NOT pg_input_is_valid(s.income, 'numeric') THEN 'INVALID_INCOME' END,
                 CASE WHEN pg_input_is_valid(s.income, 'numeric') AND (s.income::numeric < 0) THEN 'NEGATIVE_INCOME' END,
                 CASE WHEN s.opened_at IS NOT NULL AND TRIM(s.opened_at) <> '' AND pg_input_is_valid(s.opened_at, 'timestamp') = false THEN 'INVALID_OPENED_AT_FORMAT' END,
                 CASE WHEN pg_input_is_valid(s.dob, 'date') AND s.dob::date > CURRENT_DATE THEN 'FUTURE_DOB' END,
                 CASE WHEN pg_input_is_valid(s.opened_at, 'timestamp') AND s.opened_at::timestamp > CURRENT_TIMESTAMP THEN 'FUTURE_OPENED_AT' END
        ], NULL) AS non_critical_error_array

FROM staging_customers s
WHERE s.job_id = p_job_id;

INSERT INTO customer_errors (job_id, cif, raw_row, critical_errors, warning_errors)
SELECT job_id, cif, raw_row, critical_error_array, non_critical_error_array
FROM evaluated_data
WHERE cardinality(critical_error_array) > 0
   OR cardinality(non_critical_error_array) > 0;

CREATE TEMP TABLE valid_data ON COMMIT DROP AS
SELECT *
FROM evaluated_data
WHERE (cardinality(critical_error_array) = 0 OR critical_error_array IS NULL)
  AND (cardinality(non_critical_error_array) = 0 OR non_critical_error_array IS NULL);

CREATE TEMP TABLE deduped_customers ON COMMIT DROP AS
SELECT DISTINCT ON (cif)
    cif, first_name, middle_name, last_name, dob, income
FROM valid_data
ORDER BY cif, opened_at::TIMESTAMP DESC NULLS LAST;

INSERT INTO customer (
    cif, first_name, middle_name, last_name, dob, income,
    created_at, updated_at, is_deleted
)
SELECT
    cif, first_name, middle_name, last_name,
    dob::DATE, income::NUMERIC(18,2),
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
FROM deduped_customers
    ON CONFLICT (cif)
    DO UPDATE SET
    first_name = EXCLUDED.first_name,
               middle_name = EXCLUDED.middle_name,
               last_name = EXCLUDED.last_name,
               dob = EXCLUDED.dob,
               income = EXCLUDED.income,
               updated_at = CURRENT_TIMESTAMP;

INSERT INTO account (
    customer_id, account_number, account_type, opened_at,
    created_at, updated_at, is_deleted
)
SELECT DISTINCT ON (e.account_number)
    c.customer_id,
    e.account_number,
    UPPER(TRIM(e.account_type))::account_type_enum,
    e.opened_at::TIMESTAMP,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
FROM valid_data e
    JOIN customer c ON c.cif = e.cif AND c.is_deleted = false
ORDER BY e.account_number, e.opened_at::TIMESTAMP DESC NULLS LAST

ON CONFLICT (account_number)
    DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
           account_type = EXCLUDED.account_type,
           opened_at = EXCLUDED.opened_at,
           updated_at = CURRENT_TIMESTAMP;

DELETE FROM staging_customers WHERE job_id = p_job_id;

END;
$$;