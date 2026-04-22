package org.tss.tm.dto.tenant.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegistrationRequest {

    @NotBlank(message = "Tenant code is required")
    private String tenantCode;

    @NotBlank(message = "Tenant name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private UUID onboardedByAdminId;

    @NotNull(message = "Admin registration details are required")
    @Valid
    private TenantAdminRegistrationRequest adminRegistrationRequest;
}
