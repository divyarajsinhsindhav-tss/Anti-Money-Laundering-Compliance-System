package org.tss.tm.ruleEngine;

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

        return "(total_credits > 0 AND (((total_credits - total_debits) / total_credits) * 100.0 <= :" + getRuleCode() + "_MAX_RETENTION_PCT))";
    }
}