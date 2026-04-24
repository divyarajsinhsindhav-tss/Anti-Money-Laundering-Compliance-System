package org.tss.tm.dto.tenant.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class TenantAvailableResponse {
    boolean isAvailable;
}
