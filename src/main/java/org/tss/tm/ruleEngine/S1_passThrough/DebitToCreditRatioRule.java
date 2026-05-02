package org.tss.tm.ruleEngine.S1_passThrough;

import org.tss.tm.ruleEngine.AmlRule;

import java.util.Map;

public class DebitToCreditRatioRule implements AmlRule {

    @Override
    public String getRuleCode() {
        return "S1_R2_DEBIT_VS_CREDIT";
    }

    @Override
    public String getBooleanCondition(Map<String, Object> params) {
        if (!params.containsKey("MIN_DEBIT_CREDIT_RATIO_PCT")) {
            throw new IllegalArgumentException("Missing parameter: MIN_DEBIT_CREDIT_RATIO_PCT");
        }

        return """
                (
                    total_credits > 0
                    AND
                    (
                        (total_debits * 100.0 / total_credits)
                    ) >= :%s_MIN_DEBIT_CREDIT_RATIO_PCT
                )
                """.formatted(getRuleCode());
    }
}