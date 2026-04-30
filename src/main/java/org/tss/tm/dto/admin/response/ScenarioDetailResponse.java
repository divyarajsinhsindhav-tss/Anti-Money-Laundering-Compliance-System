package org.tss.tm.dto.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.StatusBasic;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDetailResponse {
    private String scenarioCode;
    private String scenarioName;
    private String description;
    private StatusBasic status;
    private List<TenantInfo> tenants;
    private List<RuleInfo> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private String tenantCode;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleInfo {
        private String ruleCode;
        private String ruleName;
        private String description;
    }
}
