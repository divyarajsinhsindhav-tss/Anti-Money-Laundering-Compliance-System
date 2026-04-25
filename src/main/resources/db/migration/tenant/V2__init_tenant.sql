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
    BEFORE INSERT ON tenant_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_tenant_user_code();

CREATE TRIGGER trg_tenant_user_updated_at
    BEFORE UPDATE ON tenant_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  customer
-- ------------------------------------------------------------
CREATE TABLE customer (
                          customer_id  UUID         NOT NULL DEFAULT uuid_generate_v7(),
                          tenant_id    UUID         NOT NULL REFERENCES public.tenant(tenant_id),

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
    CONSTRAINT uq_customer_cif   UNIQUE (tenant_id, cif)
);

CREATE TRIGGER trg_customer_updated_at
    BEFORE UPDATE ON customer
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  account
-- ------------------------------------------------------------
CREATE TABLE account (
                         account_id     UUID              NOT NULL DEFAULT uuid_generate_v7(),
                         tenant_id      UUID              NOT NULL REFERENCES public.tenant(tenant_id),
                         customer_id    UUID              NOT NULL REFERENCES customer(customer_id),

                         account_number VARCHAR(30)       NOT NULL
                             CHECK (account_number ~ '^[0-9]{6,30}$'),
    account_type   account_type_enum NOT NULL,

    is_deleted     BOOLEAN           NOT NULL DEFAULT FALSE,

    opened_at      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_account              PRIMARY KEY (account_id),
    CONSTRAINT uq_account_number       UNIQUE (tenant_id, account_number)
);

CREATE INDEX ix_account_customer ON account (customer_id) WHERE is_deleted = FALSE;


-- ------------------------------------------------------------
--  financial_transaction
-- ------------------------------------------------------------
CREATE TABLE financial_transaction (
                                       transaction_id           UUID          NOT NULL DEFAULT uuid_generate_v7(),
                                       tenant_id                UUID          NOT NULL REFERENCES public.tenant(tenant_id),
                                       batch_id                 UUID,         -- references job_execution.job_id (loose FK)

                                       txn_no                   VARCHAR(50)   NOT NULL
                                           CHECK (txn_no ~ '^[A-Z0-9\-]{3,50}$'),

    account_id               UUID          NOT NULL REFERENCES account(account_id),

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
    CONSTRAINT uq_txn_no                UNIQUE (tenant_id, txn_no)
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
    BEFORE UPDATE ON country_list
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
                        txn_id             UUID              NOT NULL REFERENCES financial_transaction(transaction_id),
                        customer_id        UUID              NOT NULL REFERENCES customer(customer_id),

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
    BEFORE INSERT ON alerts
    FOR EACH ROW EXECUTE FUNCTION trg_set_alert_code();

CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON alerts
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  aml_case
-- ------------------------------------------------------------
CREATE TABLE aml_case (
                          case_id      UUID              NOT NULL DEFAULT uuid_generate_v7(),
                          tenant_id    UUID              NOT NULL REFERENCES public.tenant(tenant_id),

                          case_code    VARCHAR(20)       NOT NULL,   -- "CSE-000001" ← trigger

                          created_by   UUID              NOT NULL REFERENCES tenant_user(user_id),
                          assigned_to  UUID                         REFERENCES tenant_user(user_id),

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
    BEFORE INSERT ON aml_case
    FOR EACH ROW EXECUTE FUNCTION trg_set_case_code();

CREATE TRIGGER trg_aml_case_updated_at
    BEFORE UPDATE ON aml_case
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();


-- ------------------------------------------------------------
--  case_alert_mapping
-- ------------------------------------------------------------
CREATE TABLE case_alert_mapping (
                                    case_alert_mapping_id UUID NOT NULL DEFAULT uuid_generate_v7(),
                                    tenant_id             UUID NOT NULL REFERENCES public.tenant(tenant_id),
                                    case_id               UUID NOT NULL REFERENCES aml_case(case_id),
                                    alert_id              UUID NOT NULL REFERENCES alerts(alert_id),

                                    CONSTRAINT pk_case_alert_mapping PRIMARY KEY (case_alert_mapping_id),
                                    CONSTRAINT uq_case_alert         UNIQUE (case_id, alert_id)
);



-- ------------------------------------------------------------
--  alert_audits
-- ------------------------------------------------------------
CREATE TABLE alert_audits (
                              alert_audit_id UUID              NOT NULL DEFAULT uuid_generate_v7(),
                              tenant_id      UUID              NOT NULL REFERENCES public.tenant(tenant_id),
                              alert_id       UUID              NOT NULL REFERENCES alerts(alert_id),

                              status_from    alert_status_enum NOT NULL,
                              status_to      alert_status_enum NOT NULL,
                              changed_by     UUID              NOT NULL REFERENCES tenant_user(user_id),
                              changed_at     TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              reason         TEXT,

                              CONSTRAINT pk_alert_audits  PRIMARY KEY (alert_audit_id),
                              CONSTRAINT chk_alert_status_changed CHECK (status_from <> status_to)
);

CREATE INDEX ix_alert_audits_alert ON alert_audits (alert_id, changed_at DESC);


-- ------------------------------------------------------------
--  case_audit
-- ------------------------------------------------------------
CREATE TABLE case_audit (
                            case_audit_id UUID             NOT NULL DEFAULT uuid_generate_v7(),
                            tenant_id     UUID             NOT NULL REFERENCES public.tenant(tenant_id),
                            case_id       UUID             NOT NULL REFERENCES aml_case(case_id),

                            status_from   case_status_enum NOT NULL,
                            status_to     case_status_enum NOT NULL,
                            changed_by    UUID             NOT NULL REFERENCES tenant_user(user_id),
                            changed_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            reason        TEXT,

                            CONSTRAINT pk_case_audit  PRIMARY KEY (case_audit_id),
                            CONSTRAINT chk_case_status_changed CHECK (status_from <> status_to)
);