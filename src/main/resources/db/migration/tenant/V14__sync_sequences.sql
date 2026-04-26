-- 1. Synchronize seq_tenant_user_code
DO $$
DECLARE
    max_val BIGINT;
BEGIN
    -- Ensure sequence exists
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'seq_tenant_user_code') THEN
        CREATE SEQUENCE seq_tenant_user_code START 1;
    END IF;

    -- Find max numeric part of USR-XXXXXX
    SELECT MAX(CAST(SUBSTRING(user_code FROM 5) AS BIGINT)) INTO max_val FROM tenant_user;
    
    IF max_val IS NOT NULL THEN
        PERFORM setval('seq_tenant_user_code', max_val);
    END IF;
END $$;

-- 2. Synchronize seq_alert_code
DO $$
DECLARE
    max_val BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'seq_alert_code') THEN
        CREATE SEQUENCE seq_alert_code START 1;
    END IF;

    SELECT MAX(CAST(SUBSTRING(alert_code FROM 5) AS BIGINT)) INTO max_val FROM alerts;
    
    IF max_val IS NOT NULL THEN
        PERFORM setval('seq_alert_code', max_val);
    END IF;
END $$;

-- 3. Synchronize seq_case_code
DO $$
DECLARE
    max_val BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'seq_case_code') THEN
        CREATE SEQUENCE seq_case_code START 1;
    END IF;

    SELECT MAX(CAST(SUBSTRING(case_code FROM 5) AS BIGINT)) INTO max_val FROM aml_case;
    
    IF max_val IS NOT NULL THEN
        PERFORM setval('seq_case_code', max_val);
    END IF;
END $$;
