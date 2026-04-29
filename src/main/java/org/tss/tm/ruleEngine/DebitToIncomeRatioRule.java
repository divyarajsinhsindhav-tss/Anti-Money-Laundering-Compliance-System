package org.tss.tm.ruleEngine;

import java.util.Map;

public class DebitToIncomeRatioRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "S1_R2_DEBIT_VS_INCOME";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (!params.containsKey("MIN_PCT_OF_INCOME")) {
            throw new IllegalArgumentException("Missing parameter: MIN_PCT_OF_INCOME");
        }
        if (!params.containsKey("MIN_INCOME_CONSTANT")) {
            throw new IllegalArgumentException("Missing parameter: MIN_INCOME_CONSTANT");
        }

        return "(total_debits >= ((:" + getRuleCode() + "_MIN_PCT_OF_INCOME / 100.0) * GREATEST(COALESCE(customerIncome, 0), :" + getRuleCode() + "_MIN_INCOME_CONSTANT)))";
    }
}