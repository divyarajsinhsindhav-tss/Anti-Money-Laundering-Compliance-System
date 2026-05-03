package org.tss.tm.dto.tenant.reporting;

import org.tss.tm.common.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantUserReportDto {

    private String userCode;
    private String fullName;
    private String email;
    private UserRole role;
}
