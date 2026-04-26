-- 1. scenario_param updates
ALTER TABLE scenario_param
    DROP CONSTRAINT IF EXISTS uq_scenario_param_v,
    DROP COLUMN IF EXISTS version,
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS valid_to TIMESTAMP;

-- 2. alerts table updates
ALTER TABLE alerts
    DROP COLUMN IF EXISTS rule_id CASCADE,
    DROP COLUMN IF EXISTS rule_param_version CASCADE,
    DROP COLUMN IF EXISTS txn_id CASCADE;

-- 3. create alert_info table
CREATE TABLE IF NOT EXISTS alert_info (
    alert_info_id  UUID NOT NULL DEFAULT public.uuid_generate_v7(),
    alert_id       UUID NOT NULL REFERENCES alerts(alert_id),
    transaction_id UUID NOT NULL REFERENCES financial_transaction(transaction_id),
    rule_id        UUID NOT NULL REFERENCES public.rules(rule_id),
    scenario_id    UUID NOT NULL REFERENCES public.scenarios(scenario_id),

    CONSTRAINT pk_alert_info PRIMARY KEY (alert_info_id),
    CONSTRAINT uq_alert_info UNIQUE (transaction_id, rule_id, scenario_id)
);
