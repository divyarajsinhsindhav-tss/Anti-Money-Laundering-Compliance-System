CREATE TABLE IF NOT EXISTS rule_engine_errors (
    error_id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) NOT NULL,
    info TEXT,
    severity VARCHAR(50),
    rule_code VARCHAR(100),
    scenario_code VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
