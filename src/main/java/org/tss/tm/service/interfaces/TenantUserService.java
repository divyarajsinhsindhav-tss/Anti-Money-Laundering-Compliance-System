package org.tss.tm.service.interfaces;

import org.tss.tm.dto.user.request.ComplianceOfficerRegistrationRequest;
import org.tss.tm.dto.user.response.UserResponse;

public interface TenantUserService {
    UserResponse registerComplianceOfficer(ComplianceOfficerRegistrationRequest request, String currentTenantSchema);
}
