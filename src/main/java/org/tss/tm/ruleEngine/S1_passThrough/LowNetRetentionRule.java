package org.tss.tm.ruleEngine.S1_passThrough;

import org.tss.tm.ruleEngine.AmlRule;

import java.util.Map;

public class LowNetRetentionRule implements AmlRule {
    @Override
    public String getRuleCode() {
        return "S1_R3_LOW_RETENTION";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (!params.containsKey("MAX_RETENTION_PCT")) {
            throw new IllegalArgumentException("Missing parameter: MAX_RETENTION_PCT");
        }

        return "(total_credits > 0 AND (((total_credits - LEAST(total_debits, total_credits) / total_credits) * 100.0 <= :" + getRuleCode() + "_MAX_RETENTION_PCT))";
    }
}