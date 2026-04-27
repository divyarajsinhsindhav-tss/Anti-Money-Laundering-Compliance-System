package org.tss.tm.dto.tenant.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequest {

    @NotEmpty(message = "Alert codes cannot be empty")
    private List<String> alertCodes;

    @NotEmpty(message = "Assigned user code cannot be empty")
    private String assignedToUserCode;

    private String notes;
}
