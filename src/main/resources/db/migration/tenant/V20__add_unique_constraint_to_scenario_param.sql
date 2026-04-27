-- Add unique index to scenario_param table for active records only
DROP INDEX IF EXISTS uq_tenant_scenario_rule_param_active;

CREATE UNIQUE INDEX uq_tenant_scenario_rule_param_active 
ON scenario_param (scenario_id, rule_id, param_key) 
WHERE valid_to IS NULL;
