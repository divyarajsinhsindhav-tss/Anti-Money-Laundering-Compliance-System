-- Update triggers to use schema-aware sequence checks to avoid "relation does not exist" errors
-- especially in multi-tenant environments where sequences might exist in other schemas.

CREATE OR REPLACE FUNCTION trg_set_tenant_user_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT := 'seq_tenant_user_code';
    next_val BIGINT;
BEGIN
    IF NEW.user_code IS NULL OR NEW.user_code = '' THEN
        -- Check for sequence in the current schema specifically
        IF NOT EXISTS (
            SELECT 1 
            FROM pg_sequences 
            WHERE sequencename = seq_name 
            AND schemaname = current_schema
        ) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.user_code := 'USR-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION trg_set_alert_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT := 'seq_alert_code';
    next_val BIGINT;
BEGIN
    IF NEW.alert_code IS NULL OR NEW.alert_code = '' THEN
        IF NOT EXISTS (
            SELECT 1 
            FROM pg_sequences 
            WHERE sequencename = seq_name 
            AND schemaname = current_schema
        ) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.alert_code := 'ALT-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION trg_set_case_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT := 'seq_case_code';
    next_val BIGINT;
BEGIN
    IF NEW.case_code IS NULL OR NEW.case_code = '' THEN
        IF NOT EXISTS (
            SELECT 1 
            FROM pg_sequences 
            WHERE sequencename = seq_name 
            AND schemaname = current_schema
        ) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.case_code := 'CSE-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;
