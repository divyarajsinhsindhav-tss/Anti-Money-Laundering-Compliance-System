CREATE
OR REPLACE PROCEDURE validate_customers(p_job_id TEXT)
LANGUAGE plpgsql
AS $$
BEGIN

CREATE TEMP TABLE evaluated_data AS
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

CREATE TEMP TABLE valid_data AS
SELECT *
FROM evaluated_data
WHERE (cardinality(critical_error_array) = 0 OR critical_error_array IS NULL)
  AND (cardinality(non_critical_error_array) = 0 OR non_critical_error_array IS NULL);

INSERT INTO customer_errors (job_id, cif, raw_row, critical_errors, warning_errors)
SELECT job_id, cif, raw_row, critical_error_array, non_critical_error_array
FROM evaluated_data
WHERE cardinality(critical_error_array) > 0
   OR cardinality(non_critical_error_array) > 0;

CREATE TEMP TABLE deduped_customers AS
SELECT DISTINCT ON (cif)
    cif, first_name, middle_name, last_name, dob, income
FROM valid_data
ORDER BY cif, opened_at DESC;

INSERT INTO customer (
    cif, first_name, middle_name, last_name, dob, income,
    created_at, updated_at, is_deleted
)
SELECT
    cif,
    first_name,
    middle_name,
    last_name,
    dob::DATE,
    income::NUMERIC(18,2),
            CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false
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
    customer_id,
    account_number,
    account_type,
    opened_at,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    c.customer_id,
    e.account_number,
    e.account_type::account_type_enum,
    e.opened_at::TIMESTAMP,
            CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false
FROM valid_data e
         JOIN customer c
              ON c.cif = e.cif
                  AND c.is_deleted = false
    ON CONFLICT (account_number)
DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
           account_type = EXCLUDED.account_type,
           opened_at = EXCLUDED.opened_at,
           updated_at = CURRENT_TIMESTAMP;

DELETE
FROM staging_customers
WHERE job_id = p_job_id;

END;
$$;

CREATE INDEX ON staging_customers(job_id);
CREATE INDEX ON customer(cif);
CREATE INDEX ON account(account_number);
