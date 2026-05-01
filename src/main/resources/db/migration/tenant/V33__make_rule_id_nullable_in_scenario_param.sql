-- Make rule_id nullable to allow common (scenario-level) parameters in scenario_param table
-- This is a repeat of the logic in V32 to ensure it is applied if V32 was partially executed or skipped
ALTER TABLE scenario_param 
    ALTER COLUMN rule_id DROP NOT NULL;
