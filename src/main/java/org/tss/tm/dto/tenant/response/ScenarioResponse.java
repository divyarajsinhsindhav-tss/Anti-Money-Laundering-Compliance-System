package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.StatusBasic;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioResponse {
    private UUID scenarioId;
    private String scenarioCode;
    private String scenarioName;
    private String description;
    private StatusBasic status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
