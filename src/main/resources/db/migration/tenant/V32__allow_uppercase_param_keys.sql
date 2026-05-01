-- Update scenario_param_param_key_check to allow uppercase alphanumeric keys with underscores
ALTER TABLE scenario_param
    DROP CONSTRAINT IF EXISTS scenario_param_param_key_check;

ALTER TABLE scenario_param
    ADD CONSTRAINT scenario_param_param_key_check
    CHECK (param_key ~ '^[A-Za-z][A-Za-z0-9_]{0,99}$');

-- Make rule_id nullable to allow common (scenario-level) parameters
ALTER TABLE scenario_param
    ALTER COLUMN rule_id DROP NOT NULL;
