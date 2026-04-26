package org.tss.tm.ruleEngine;

import java.util.Map;

public class HighDebitTurnoverRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "RULE_HIGH_DEBIT";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (params == null || !params.containsKey("MIN_DEBIT_TURNOVER")) {
            throw new IllegalArgumentException("Missing required parameter: MIN_DEBIT_TURNOVER");
        }
        return "(total_debits >= :" + getRuleCode() + "_MIN_DEBIT_TURNOVER)";
    }
}