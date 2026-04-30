-- Increase length of rule_code and scenario_code to accommodate descriptive identifiers
ALTER TABLE public.rules
    ALTER COLUMN rule_code TYPE VARCHAR(100);

ALTER TABLE public.scenarios
    ALTER COLUMN scenario_code TYPE VARCHAR(100);
