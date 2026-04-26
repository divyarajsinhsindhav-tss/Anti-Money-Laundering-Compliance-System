-- Add IN_CASE to alert_status_enum
ALTER TYPE alert_status_enum ADD VALUE IF NOT EXISTS 'IN_CASE';
