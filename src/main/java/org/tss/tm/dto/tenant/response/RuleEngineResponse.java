package org.tss.tm.dto.tenant.response;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class RuleEngineResponse {
    private String jobId;
}
