package org.tss.tm.dto.tenant.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlJobRequest {
    @NotBlank(message = "Tenant code is required")
    private LocalDate fromDate;

    @NotBlank(message = "Tenant code is required")
    private LocalDate toDate;
}
