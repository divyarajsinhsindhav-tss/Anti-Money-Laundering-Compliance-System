package org.tss.tm.dto.tenant.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tss.tm.common.enums.AlertStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAlertStatusRequest {

    @NotNull(message = "Status cannot be null")
    private AlertStatus status;

    private String reason;
}
