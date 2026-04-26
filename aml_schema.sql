-- ============================================================
--  AML PLATFORM — FULL DATABASE SCHEMA
--  • UUID v7  (time-ordered, B-tree index friendly)
--  • Auto-generated codes via INSERT triggers
--  • ENUMs for all categorical columns
--  • CHECK + regex constraints
--  • Sequences for human-readable codes
--  • is_deleted where logical deletion applies
-- ============================================================


-- ============================================================
--  UUID v7 HELPER
--  Time-ordered: first 48 bits = ms timestamp → index friendly.
--  Requires pgcrypto (gen_random_bytes).
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION uuid_generate_v7()
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    unix_ms  BIGINT;
    rand_bytes BYTEA;
    hex_str  TEXT;
BEGIN
    unix_ms    := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;
    rand_bytes := gen_random_bytes(10);

    hex_str :=
        lpad(to_hex(unix_ms), 12, '0') ||          -- 48-bit timestamp
        '7' ||                                      -- version nibble
        lpad(to_hex(get_byte(rand_bytes, 0) & 15 | 64), 3, '0') ||   -- rand_a (12 bits, version=7)
        lpad(to_hex(get_byte(rand_bytes, 1) & 63  | 128), 2, '0') || -- variant bits
        encode(substr(rand_bytes, 3, 8), 'hex');   -- rand_b (64 bits)

    RETURN CAST(
        substr(hex_str,  1, 8) || '-' ||
        substr(hex_str,  9, 4) || '-' ||
        substr(hex_str, 13, 4) || '-' ||
        substr(hex_str, 17, 4) || '-' ||
        substr(hex_str, 21, 12)
    AS UUID);
END;
$$;


-- ============================================================
--  ENUMS  (public schema — shared across tenants)
-- ============================================================

-- General
CREATE TYPE status_basic      AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE status_full       AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED');
CREATE TYPE job_type_enum     AS ENUM ('FILE_UPLOAD_CUSTOMER', 'FILE_UPLOAD_TRANSACTION', 'RULE_ENGINE');
CREATE TYPE job_status_enum   AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED');
CREATE TYPE direction_enum    AS ENUM ('IN', 'OUT');
CREATE TYPE risk_enum         AS ENUM ('HIGH', 'MEDIUM', 'LOW');
CREATE TYPE data_type_enum    AS ENUM ('STRING', 'INT', 'DECIMAL', 'BOOLEAN', 'DATE');
CREATE TYPE account_type_enum AS ENUM ('SAVINGS', 'CURRENT', 'LOAN', 'FIXED_DEPOSIT', 'WALLET');
CREATE TYPE txn_type_enum     AS ENUM ('NEFT', 'RTGS', 'IMPS', 'UPI', 'CASH', 'CHEQUE', 'INTERNAL', 'WIRE');
CREATE TYPE rule_category_enum AS ENUM ('VELOCITY', 'THRESHOLD', 'GEOGRAPHY', 'PATTERN', 'COUNTERPARTY', 'BEHAVIOUR');

-- Users & Roles
CREATE TYPE user_role_enum    AS ENUM ('SYSTEM_ADMIN', 'BANK_ADMIN', 'COMPLIANCE_OFFICER');

-- Alerts
CREATE TYPE alert_status_enum AS ENUM ('OPEN', 'UNDER_REVIEW', 'ESCALATED', 'CLOSED_TRUE_POSITIVE', 'CLOSED_FALSE_POSITIVE', 'CLOSED_INCONCLUSIVE');

-- Cases
CREATE TYPE case_status_enum  AS ENUM ('OPEN', 'UNDER_REVIEW', 'ESCALATED', 'CLOSED_SAR_FILED', 'CLOSED_NO_ACTION', 'CLOSED_INCONCLUSIVE');

-- Audit
CREATE TYPE action_type_enum  AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'EXPORT', 'STATUS_CHANGE', 'ASSIGN');
CREATE TYPE entity_type_enum  AS ENUM ('TENANT', 'USER', 'CUSTOMER', 'ACCOUNT', 'TRANSACTION', 'ALERT', 'CASE', 'SCENARIO', 'RULE', 'JOB');

-- Tenant scenario
CREATE TYPE tenant_status_enum AS ENUM ('ONBOARDING', 'ACTIVE', 'SUSPENDED', 'OFFBOARDED');


-- ============================================================
--  SEQUENCES  (human-readable codes)
-- ============================================================
CREATE SEQUENCE seq_admin_code       START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
CREATE SEQUENCE seq_tenant_code      START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
CREATE SEQUENCE seq_scenario_code    START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
CREATE SEQUENCE seq_rule_code        START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
CREATE SEQUENCE seq_alert_code       START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
CREATE SEQUENCE seq_case_code        START 1 INCREMENT 1 MINVALUE 1 NO MAXVALUE;
-- Per-tenant sequences are created dynamically when a tenant is onboarded.
-- Example: seq_txn_<tenant_id>, seq_cif_<tenant_id>


-- ============================================================
--  PUBLIC SCHEMA TABLES
-- ============================================================

-- ------------------------------------------------------------
--  system_admin
-- ------------------------------------------------------------
CREATE TABLE public.system_admin (
    system_admin_id   UUID        NOT NULL DEFAULT uuid_generate_v7(),
    system_admin_code VARCHAR(20) NOT NULL UNIQUE,   -- e.g. "SADM-000001"  ← trigger

    first_name        VARCHAR(100) NOT NULL
        CHECK (first_name  ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),
    middle_name       VARCHAR(100)
        CHECK (middle_name IS NULL OR middle_name ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),
    last_name         VARCHAR(100) NOT NULL
        CHECK (last_name   ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),

    phone_number      VARCHAR(20) NOT NULL
        CHECK (phone_number ~ '^\+?[1-9][0-9]{6,18}$'),
    email             VARCHAR(255) NOT NULL UNIQUE
        CHECK (email ~ '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$'),

    password_hash     VARCHAR(512) NOT NULL,

    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_system_admin PRIMARY KEY (system_admin_id)
);

-- Trigger: auto-generate system_admin_code on INSERT
CREATE OR REPLACE FUNCTION trg_set_system_admin_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.system_admin_code IS NULL OR NEW.system_admin_code = '' THEN
        NEW.system_admin_code := 'SADM-' || lpad(nextval('seq_admin_code')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_system_admin_code_insert
    BEFORE INSERT ON public.system_admin
    FOR EACH ROW EXECUTE FUNCTION trg_set_system_admin_code();

CREATE TRIGGER trg_system_admin_updated_at
    BEFORE UPDATE ON public.system_admin
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  tenant
-- ------------------------------------------------------------
CREATE TABLE public.tenant (
    tenant_id           UUID              NOT NULL DEFAULT uuid_generate_v7(),
    tenant_code         VARCHAR(20)       NOT NULL UNIQUE,   -- "TNT-000001" ← trigger

    name                VARCHAR(150)      NOT NULL
        CHECK (name ~ '^[A-Za-z0-9][A-Za-z0-9 _\-\.]{1,148}$'),
    display_name        VARCHAR(200)      NOT NULL,
    schema_name         VARCHAR(63)       NOT NULL UNIQUE
        CHECK (schema_name ~ '^[a-z][a-z0-9_]{1,61}[a-z0-9]$'),   -- valid pg schema name

    status              tenant_status_enum NOT NULL DEFAULT 'ONBOARDING',
    is_deleted          BOOLEAN            NOT NULL DEFAULT FALSE,

    onboarded_by_admin_id UUID            NOT NULL
        REFERENCES public.system_admin(system_admin_id),

    created_at          TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_tenant PRIMARY KEY (tenant_id)
);

CREATE OR REPLACE FUNCTION trg_set_tenant_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.tenant_code IS NULL OR NEW.tenant_code = '' THEN
        NEW.tenant_code := 'TNT-' || lpad(nextval('seq_tenant_code')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_tenant_code_insert
    BEFORE INSERT ON public.tenant
    FOR EACH ROW EXECUTE FUNCTION trg_set_tenant_code();

CREATE TRIGGER trg_tenant_updated_at
    BEFORE UPDATE ON public.tenant
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  scenarios
-- ------------------------------------------------------------
CREATE TABLE public.scenarios (
    scenario_id    UUID              NOT NULL DEFAULT uuid_generate_v7(),
    scenario_code  VARCHAR(20)       NOT NULL UNIQUE,   -- "SCN-000001" ← trigger

    name           VARCHAR(200)      NOT NULL,
    description    TEXT,
    status         status_basic      NOT NULL DEFAULT 'ACTIVE',
    is_deleted     BOOLEAN           NOT NULL DEFAULT FALSE,

    created_by     UUID              NOT NULL
        REFERENCES public.system_admin(system_admin_id),

    created_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_scenarios PRIMARY KEY (scenario_id)
);

CREATE OR REPLACE FUNCTION trg_set_scenario_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.scenario_code IS NULL OR NEW.scenario_code = '' THEN
        NEW.scenario_code := 'SCN-' || lpad(nextval('seq_scenario_code')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_scenario_code_insert
    BEFORE INSERT ON public.scenarios
    FOR EACH ROW EXECUTE FUNCTION trg_set_scenario_code();

CREATE TRIGGER trg_scenarios_updated_at
    BEFORE UPDATE ON public.scenarios
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  rules
-- ------------------------------------------------------------
CREATE TABLE public.rules (
    rule_id        UUID               NOT NULL DEFAULT uuid_generate_v7(),
    rule_code      VARCHAR(20)        NOT NULL UNIQUE,   -- "RUL-000001" ← trigger

    rule_name      VARCHAR(200)       NOT NULL,
    description    TEXT,
    rule_category  rule_category_enum NOT NULL,
    status         status_basic       NOT NULL DEFAULT 'ACTIVE',
    is_deleted     BOOLEAN            NOT NULL DEFAULT FALSE,

    created_by     UUID               NOT NULL
        REFERENCES public.system_admin(system_admin_id),

    created_at     TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rules PRIMARY KEY (rule_id)
);

CREATE OR REPLACE FUNCTION trg_set_rule_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.rule_code IS NULL OR NEW.rule_code = '' THEN
        NEW.rule_code := 'RUL-' || lpad(nextval('seq_rule_code')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_rule_code_insert
    BEFORE INSERT ON public.rules
    FOR EACH ROW EXECUTE FUNCTION trg_set_rule_code();

CREATE TRIGGER trg_rules_updated_at
    BEFORE UPDATE ON public.rules
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  scenario_rule_mapping
-- ------------------------------------------------------------
CREATE TABLE public.scenario_rule_mapping (
    scenario_rule_mapping_id UUID NOT NULL DEFAULT uuid_generate_v7(),
    scenario_id UUID NOT NULL REFERENCES public.scenarios(scenario_id),
    rule_id     UUID NOT NULL REFERENCES public.rules(rule_id),

    CONSTRAINT pk_scenario_rule_mapping PRIMARY KEY (scenario_rule_mapping_id),
    CONSTRAINT uq_scenario_rule         UNIQUE (scenario_id, rule_id)
);


-- ------------------------------------------------------------
--  tenant_scenario_mapping
-- ------------------------------------------------------------
CREATE TABLE public.tenant_scenario_mapping (
    tenant_scenario_mapping_id UUID      NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id                  UUID      NOT NULL REFERENCES public.tenant(tenant_id),
    scenario_id                UUID      NOT NULL REFERENCES public.scenarios(scenario_id),

    is_enabled                 BOOLEAN   NOT NULL DEFAULT TRUE,
    activated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at             TIMESTAMP,

    CONSTRAINT pk_tenant_scenario_mapping PRIMARY KEY (tenant_scenario_mapping_id),
    CONSTRAINT uq_tenant_scenario         UNIQUE (tenant_id, scenario_id),
    CONSTRAINT chk_deactivated_after_activated
        CHECK (deactivated_at IS NULL OR deactivated_at > activated_at)
);


-- ------------------------------------------------------------
--  job_execution
-- ------------------------------------------------------------
CREATE TABLE public.job_execution (
    job_id       UUID             NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id    UUID             NOT NULL REFERENCES public.tenant(tenant_id),

    job_type     job_type_enum    NOT NULL,
    status       job_status_enum  NOT NULL DEFAULT 'PENDING',

    started_at   TIMESTAMP,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_job_execution PRIMARY KEY (job_id),
    CONSTRAINT chk_job_completed_after_started
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);


-- ------------------------------------------------------------
--  audit_logs
-- ------------------------------------------------------------
CREATE TABLE public.audit_logs (
    audit_log_id UUID              NOT NULL DEFAULT uuid_generate_v7(),

    actor_id     UUID              NOT NULL,   -- system_admin OR tenant_user id
    action_type  action_type_enum  NOT NULL,
    entity_type  entity_type_enum  NOT NULL,
    entity_id    UUID              NOT NULL,

    old_value    JSONB,
    new_value    JSONB,

    ip_address   INET,
    created_at   TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_logs PRIMARY KEY (audit_log_id)
);


-- ------------------------------------------------------------
--  email_notification
-- ------------------------------------------------------------
CREATE TABLE public.email_notification (
    email_notification_id UUID         NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id             UUID         NOT NULL REFERENCES public.tenant(tenant_id),

    recipient_email       VARCHAR(255) NOT NULL
        CHECK (recipient_email ~ '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$'),
    recipient_user_id     UUID,        -- nullable: system-level emails have no user

    subject               VARCHAR(500) NOT NULL,
    template              TEXT         NOT NULL,

    sent_at               TIMESTAMP,
    is_sent               BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_email_notification PRIMARY KEY (email_notification_id)
);


-- ============================================================
--  SHARED updated_at TRIGGER FUNCTION  (used by all tables)
-- ============================================================
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


-- ============================================================
--  TENANT-SCHEMA TABLES
--  In production these live in per-tenant schemas.
--  Here written in public for clarity; substitute schema_name.
-- ============================================================

-- ------------------------------------------------------------
--  tenant_user
-- ------------------------------------------------------------
CREATE TABLE tenant_user (
    user_id             UUID           NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id           UUID           NOT NULL REFERENCES public.tenant(tenant_id),

    user_code           VARCHAR(20)    NOT NULL,         -- "USR-000001" ← trigger
    role                user_role_enum NOT NULL,

    first_name          VARCHAR(100)   NOT NULL
        CHECK (first_name  ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),
    last_name           VARCHAR(100)   NOT NULL
        CHECK (last_name   ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),

    phone_number        VARCHAR(20)
        CHECK (phone_number IS NULL OR phone_number ~ '^\+?[1-9][0-9]{6,18}$'),
    email               VARCHAR(255)   NOT NULL
        CHECK (email ~ '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$'),

    password_hash       VARCHAR(512)   NOT NULL,

    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,

    last_login          TIMESTAMP,
    failed_login_count  INT            NOT NULL DEFAULT 0
        CHECK (failed_login_count >= 0),
    locked_until        TIMESTAMP,

    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_tenant_user         PRIMARY KEY (user_id),
    CONSTRAINT uq_tenant_user_email   UNIQUE (tenant_id, email),
    CONSTRAINT uq_tenant_user_code    UNIQUE (tenant_id, user_code)
);

-- Trigger: auto-generate user_code per tenant using a dynamic sequence
CREATE OR REPLACE FUNCTION trg_set_tenant_user_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT;
    next_val BIGINT;
BEGIN
    IF NEW.user_code IS NULL OR NEW.user_code = '' THEN
        seq_name := 'seq_usr_' || replace(NEW.tenant_id::TEXT, '-', '_');
        -- Create per-tenant sequence on first use
        IF NOT EXISTS (
            SELECT 1 FROM pg_sequences WHERE sequencename = seq_name
        ) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.user_code := 'USR-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_tenant_user_code_insert
    BEFORE INSERT ON public.tenant_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_tenant_user_code();

CREATE TRIGGER trg_tenant_user_updated_at
    BEFORE UPDATE ON public.tenant_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  customer
-- ------------------------------------------------------------
CREATE TABLE customer (
    customer_id  UUID         NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id    UUID         REFERENCES public.tenant(tenant_id),

    cif          VARCHAR(50)  NOT NULL
        CHECK (cif ~ '^[A-Z0-9\-]{3,50}$'),

    first_name   VARCHAR(100) NOT NULL
        CHECK (first_name  ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),
    middle_name  VARCHAR(100)
        CHECK (middle_name IS NULL OR middle_name ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),
    last_name    VARCHAR(100) NOT NULL
        CHECK (last_name   ~ '^[A-Za-z][A-Za-z'' \-]{0,98}$'),

    dob          DATE         NOT NULL
        CHECK (dob BETWEEN '1900-01-01' AND CURRENT_DATE - INTERVAL '1 day'),
    income       NUMERIC(18,2)
        CHECK (income IS NULL OR income >= 0),

    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_customer       PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_cif   UNIQUE (cif)
);

CREATE TRIGGER trg_customer_updated_at
    BEFORE UPDATE ON public.customer
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  account
-- ------------------------------------------------------------
CREATE TABLE account (
    account_id     UUID              NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id      UUID              REFERENCES public.tenant(tenant_id),
    customer_id    UUID              NOT NULL REFERENCES public.customer(customer_id),

    account_number VARCHAR(30)       NOT NULL
        CHECK (account_number ~ '^[0-9]{6,30}$'),
    account_type   account_type_enum NOT NULL,

    is_deleted     BOOLEAN           NOT NULL DEFAULT FALSE,

    opened_at      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_account              PRIMARY KEY (account_id),
    CONSTRAINT uq_account_number       UNIQUE (account_number)
);

CREATE INDEX ix_account_customer ON public.account (customer_id) WHERE is_deleted = FALSE;


-- ------------------------------------------------------------
--  financial_transaction
-- ------------------------------------------------------------
CREATE TABLE financial_transaction (
    transaction_id           UUID          NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id                UUID          REFERENCES public.tenant(tenant_id),
    batch_id                 UUID,         -- references job_execution.job_id (loose FK)

    txn_no                   VARCHAR(50)   NOT NULL
        CHECK (txn_no ~ '^[A-Z0-9\-]{3,50}$'),

    account_id               UUID          NOT NULL REFERENCES public.account(account_id),

    amount                   NUMERIC(20,4) NOT NULL
        CHECK (amount > 0),
    txn_type                 txn_type_enum NOT NULL,
    direction                direction_enum NOT NULL,

    counterparty_account_no  VARCHAR(30)
        CHECK (counterparty_account_no IS NULL OR counterparty_account_no ~ '^[0-9]{4,30}$'),
    counterparty_bank_ifsc   VARCHAR(11)
        CHECK (counterparty_bank_ifsc IS NULL OR counterparty_bank_ifsc ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),

    txn_timestamp            TIMESTAMP     NOT NULL,
    country_code             CHAR(2)       NOT NULL
        CHECK (country_code ~ '^[A-Z]{2}$'),   -- ISO 3166-1 alpha-2

    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_financial_transaction PRIMARY KEY (transaction_id),
    CONSTRAINT uq_txn_no                UNIQUE (txn_no)
);

-- ------------------------------------------------------------
--  country_list
-- ------------------------------------------------------------
CREATE TABLE country_list (
    country_id   UUID        NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id    UUID        NOT NULL REFERENCES public.tenant(tenant_id),

    country_code CHAR(2)     NOT NULL
        CHECK (country_code ~ '^[A-Z]{2}$'),
    country_name VARCHAR(100) NOT NULL,
    risk         risk_enum    NOT NULL DEFAULT 'LOW',

    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_country_list        PRIMARY KEY (country_id),
    CONSTRAINT uq_country_per_tenant  UNIQUE (tenant_id, country_code)
);

CREATE TRIGGER trg_country_list_updated_at
    BEFORE UPDATE ON public.country_list
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  scenario_param
-- ------------------------------------------------------------
CREATE TABLE scenario_param (
    scenario_param_id UUID           NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id         UUID           NOT NULL REFERENCES public.tenant(tenant_id),
    scenario_id       UUID           NOT NULL REFERENCES public.scenarios(scenario_id),
    rule_id           UUID           NOT NULL REFERENCES public.rules(rule_id),

    param_key         VARCHAR(100)   NOT NULL
        CHECK (param_key ~ '^[a-z][a-z0-9_]{1,98}$'),   -- snake_case keys only
    data_type         data_type_enum NOT NULL,

    string_value      TEXT,
    int_value         BIGINT,
    decimal_value     NUMERIC(20,6),

    -- At least one value column must be non-null
    CONSTRAINT chk_scenario_param_value
        CHECK (
            (data_type = 'STRING'  AND string_value  IS NOT NULL) OR
            (data_type = 'INT'     AND int_value      IS NOT NULL) OR
            (data_type = 'DECIMAL' AND decimal_value  IS NOT NULL) OR
            (data_type IN ('BOOLEAN','DATE') AND string_value IS NOT NULL)
        ),

    version           INT            NOT NULL DEFAULT 1
        CHECK (version > 0),

    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_scenario_param    PRIMARY KEY (scenario_param_id),
    CONSTRAINT uq_scenario_param_v  UNIQUE (tenant_id, scenario_id, rule_id, param_key, version)
);


-- ------------------------------------------------------------
--  alerts
-- ------------------------------------------------------------
CREATE TABLE alerts (
    alert_id           UUID              NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id          UUID              NOT NULL REFERENCES public.tenant(tenant_id),

    alert_code         VARCHAR(20)       NOT NULL,   -- "ALT-000001" ← trigger

    job_id             UUID              NOT NULL REFERENCES public.job_execution(job_id),
    rule_id            UUID              NOT NULL REFERENCES public.rules(rule_id),
    rule_param_version INT               NOT NULL CHECK (rule_param_version > 0),
    scenario_id        UUID              NOT NULL REFERENCES public.scenarios(scenario_id),
    txn_id             UUID              NOT NULL REFERENCES public.financial_transaction(transaction_id),
    customer_id        UUID              NOT NULL REFERENCES public.customer(customer_id),

    alert_status       alert_status_enum NOT NULL DEFAULT 'OPEN',
    is_deleted         BOOLEAN           NOT NULL DEFAULT FALSE,

    created_at         TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_alerts      PRIMARY KEY (alert_id),
    CONSTRAINT uq_alert_code  UNIQUE (tenant_id, alert_code)
);

-- Trigger: auto-generate alert_code per tenant
CREATE OR REPLACE FUNCTION trg_set_alert_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT;
    next_val BIGINT;
BEGIN
    IF NEW.alert_code IS NULL OR NEW.alert_code = '' THEN
        seq_name := 'seq_alt_' || replace(NEW.tenant_id::TEXT, '-', '_');
        IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = seq_name) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.alert_code := 'ALT-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_alert_code_insert
    BEFORE INSERT ON public.alerts
    FOR EACH ROW EXECUTE FUNCTION trg_set_alert_code();

CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON public.alerts
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  aml_case
-- ------------------------------------------------------------
CREATE TABLE aml_case (
    case_id      UUID              NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id    UUID              NOT NULL REFERENCES public.tenant(tenant_id),

    case_code    VARCHAR(20)       NOT NULL,   -- "CSE-000001" ← trigger

    created_by   UUID              NOT NULL REFERENCES public.tenant_user(user_id),
    assigned_to  UUID                         REFERENCES public.tenant_user(user_id),

    status       case_status_enum  NOT NULL DEFAULT 'OPEN',
    notes        TEXT,

    is_deleted   BOOLEAN           NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at    TIMESTAMP,

    CONSTRAINT pk_aml_case    PRIMARY KEY (case_id),
    CONSTRAINT uq_case_code   UNIQUE (tenant_id, case_code),
    CONSTRAINT chk_closed_at  CHECK (closed_at IS NULL OR closed_at >= created_at)
);

-- Trigger: auto-generate case_code per tenant
CREATE OR REPLACE FUNCTION trg_set_case_code()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    seq_name TEXT;
    next_val BIGINT;
BEGIN
    IF NEW.case_code IS NULL OR NEW.case_code = '' THEN
        seq_name := 'seq_cse_' || replace(NEW.tenant_id::TEXT, '-', '_');
        IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = seq_name) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1 INCREMENT 1', seq_name);
        END IF;
        EXECUTE format('SELECT nextval(%L)', seq_name) INTO next_val;
        NEW.case_code := 'CSE-' || lpad(next_val::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_case_code_insert
    BEFORE INSERT ON public.aml_case
    FOR EACH ROW EXECUTE FUNCTION trg_set_case_code();

CREATE TRIGGER trg_aml_case_updated_at
    BEFORE UPDATE ON public.aml_case
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  case_alert_mapping
-- ------------------------------------------------------------
CREATE TABLE case_alert_mapping (
    case_alert_mapping_id UUID NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id             UUID NOT NULL REFERENCES public.tenant(tenant_id),
    case_id               UUID NOT NULL REFERENCES public.aml_case(case_id),
    alert_id              UUID NOT NULL REFERENCES public.alerts(alert_id),

    CONSTRAINT pk_case_alert_mapping PRIMARY KEY (case_alert_mapping_id),
    CONSTRAINT uq_case_alert         UNIQUE (case_id, alert_id)
);



-- ------------------------------------------------------------
--  alert_audits
-- ------------------------------------------------------------
CREATE TABLE alert_audits (
    alert_audit_id UUID              NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id      UUID              NOT NULL REFERENCES public.tenant(tenant_id),
    alert_id       UUID              NOT NULL REFERENCES public.alerts(alert_id),

    status_from    alert_status_enum NOT NULL,
    status_to      alert_status_enum NOT NULL,
    changed_by     UUID              NOT NULL REFERENCES public.tenant_user(user_id),
    changed_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason         TEXT,

    CONSTRAINT pk_alert_audits  PRIMARY KEY (alert_audit_id),
    CONSTRAINT chk_alert_status_changed CHECK (status_from <> status_to)
);

CREATE INDEX ix_alert_audits_alert ON public.alert_audits (alert_id, changed_at DESC);


-- ------------------------------------------------------------
--  case_audit
-- ------------------------------------------------------------
CREATE TABLE case_audit (
    case_audit_id UUID             NOT NULL DEFAULT uuid_generate_v7(),
    tenant_id     UUID             NOT NULL REFERENCES public.tenant(tenant_id),
    case_id       UUID             NOT NULL REFERENCES public.aml_case(case_id),

    status_from   case_status_enum NOT NULL,
    status_to     case_status_enum NOT NULL,
    changed_by    UUID             NOT NULL REFERENCES public.tenant_user(user_id),
    changed_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason        TEXT,

    CONSTRAINT pk_case_audit  PRIMARY KEY (case_audit_id),
    CONSTRAINT chk_case_status_changed CHECK (status_from <> status_to)
);



-- ============================================================
--  END OF SCHEMA
-- ============================================================
