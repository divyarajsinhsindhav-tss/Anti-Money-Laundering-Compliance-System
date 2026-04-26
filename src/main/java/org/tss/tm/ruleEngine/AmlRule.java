package org.tss.tm.ruleEngine;

import java.util.Map;

public interface AmlRule {
    String getRuleCode();

    String getBooleanCondition(Map<String, Object> ruleParams);
}
