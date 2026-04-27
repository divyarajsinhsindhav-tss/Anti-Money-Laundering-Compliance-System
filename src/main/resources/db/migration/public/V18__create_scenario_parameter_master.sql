-- ------------------------------------------------------------
--  scenario_parameter_master
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.scenario_parameter_master (
    parameter_id        UUID            NOT NULL DEFAULT uuid_generate_v7(),
    scenario_id         UUID            NOT NULL REFERENCES public.scenarios(scenario_id),
    rule_id             UUID            REFERENCES public.rules(rule_id),

    parameter_key       VARCHAR(100)    NOT NULL,
    display_name        VARCHAR(200)    NOT NULL,
    description         TEXT,
    data_type           data_type_enum  NOT NULL,
    default_value       VARCHAR(255),
    is_mandatory        BOOLEAN         NOT NULL DEFAULT TRUE,

    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_scenario_parameter_master PRIMARY KEY (parameter_id),
    CONSTRAINT uq_scenario_rule_param_key   UNIQUE (scenario_id, rule_id, parameter_key)
);

DROP TRIGGER IF EXISTS trg_scenario_parameter_master_updated_at ON public.scenario_parameter_master;
CREATE TRIGGER trg_scenario_parameter_master_updated_at
    BEFORE UPDATE ON public.scenario_parameter_master
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();
