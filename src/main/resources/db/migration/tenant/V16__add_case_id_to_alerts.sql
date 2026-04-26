-- Add case_id to alerts table to support direct relationship
ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS case_id UUID REFERENCES aml_case(case_id);

-- Remove tenant_id from alert_audits as it's redundant in tenant schema and removed from Java entity
ALTER TABLE alert_audits DROP COLUMN IF EXISTS tenant_id;
