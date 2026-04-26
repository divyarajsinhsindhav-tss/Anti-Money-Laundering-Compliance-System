package org.tss.tm.ruleEngine;


import java.util.Map;

public class HighCreditTurnoverRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "RULE_HIGH_CREDIT";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (!params.containsKey("MIN_CREDIT_TURNOVER")) {
            throw new IllegalArgumentException("Missing parameter: MIN_CREDIT_TURNOVER");
        }
        return "(total_credits >= :" + getRuleCode() + "_MIN_CREDIT_TURNOVER)";
    }
}