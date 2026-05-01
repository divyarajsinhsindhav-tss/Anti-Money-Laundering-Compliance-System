package org.tss.tm.ruleEngine.S1_passThrough;

import org.tss.tm.ruleEngine.AmlRule;

import java.util.Map;

public class CreditToIncomeRatioRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "S1_R1_CREDIT_VS_INCOME";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (!params.containsKey("MIN_PCT_OF_INCOME")) {
            throw new IllegalArgumentException("Missing parameter: MIN_PCT_OF_INCOME");
        }
        if (!params.containsKey("MIN_INCOME_CONSTANT")) {
            throw new IllegalArgumentException("Missing parameter: MIN_INCOME_CONSTANT");
        }

        return "(total_credits >= ((:" + getRuleCode() + "_MIN_PCT_OF_INCOME / 100.0) * GREATEST(COALESCE(customerIncome, 0), :" + getRuleCode() + "_MIN_INCOME_CONSTANT)))";
    }
}