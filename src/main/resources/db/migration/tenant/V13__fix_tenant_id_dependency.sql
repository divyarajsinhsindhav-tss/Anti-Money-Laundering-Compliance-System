-- 1. Update triggers to not depend on tenant_id for sequence names
-- Since we are in a tenant-specific schema, the sequence itself is already isolated.

CREATE OR REPLACE FUNCTION trg_set_tenant_user_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT := 'seq_tenant_user_code';
    next_val BIGINT;
BEGIN
    IF NEW.user_code IS NULL OR NEW.user_code = '' THEN
        IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = seq_name) THEN
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
        IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = seq_name) THEN
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
        IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = seq_name) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.case_code := 'CSE-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

-- 2. Make tenant_id nullable and update constraints in tenant tables
-- tenant_user
ALTER TABLE tenant_user ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE tenant_user DROP CONSTRAINT IF EXISTS uq_tenant_user_email;
ALTER TABLE tenant_user ADD CONSTRAINT uq_tenant_user_email UNIQUE (email);
ALTER TABLE tenant_user DROP CONSTRAINT IF EXISTS uq_tenant_user_code;
ALTER TABLE tenant_user ADD CONSTRAINT uq_tenant_user_code UNIQUE (user_code);

-- customer
ALTER TABLE customer ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS uq_customer_cif;
ALTER TABLE customer DROP CONSTRAINT IF EXISTS uk_tenant_cif;
ALTER TABLE customer ADD CONSTRAINT uq_customer_cif UNIQUE (cif);

-- account
ALTER TABLE account ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE account DROP CONSTRAINT IF EXISTS uq_account_number;
ALTER TABLE account DROP CONSTRAINT IF EXISTS uk_tenant_account_no;
ALTER TABLE account ADD CONSTRAINT uq_account_number UNIQUE (account_number);

-- financial_transaction
ALTER TABLE financial_transaction ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE financial_transaction DROP CONSTRAINT IF EXISTS uq_txn_no;
ALTER TABLE financial_transaction DROP CONSTRAINT IF EXISTS uk_tenant_txn_no;
ALTER TABLE financial_transaction ADD CONSTRAINT uq_txn_no UNIQUE (txn_no);

-- country_list
ALTER TABLE country_list ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE country_list DROP CONSTRAINT IF EXISTS uq_country_per_tenant;
ALTER TABLE country_list ADD CONSTRAINT uq_country_code UNIQUE (country_code);

-- scenario_param
ALTER TABLE scenario_param ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE scenario_param DROP CONSTRAINT IF EXISTS uq_scenario_param_v;

-- alerts
ALTER TABLE alerts ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE alerts DROP CONSTRAINT IF EXISTS uq_alert_code;
ALTER TABLE alerts ADD CONSTRAINT uq_alert_code UNIQUE (alert_code);

-- aml_case
ALTER TABLE aml_case ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE aml_case DROP CONSTRAINT IF EXISTS uq_case_code;
ALTER TABLE aml_case ADD CONSTRAINT uq_case_code UNIQUE (case_code);

-- mapping and audit tables
ALTER TABLE case_alert_mapping ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE alert_audits ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE case_audit ALTER COLUMN tenant_id DROP NOT NULL;
