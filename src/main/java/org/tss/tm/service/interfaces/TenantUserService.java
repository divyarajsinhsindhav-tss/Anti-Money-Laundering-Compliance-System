package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.response.TenantUserResponse;
import org.tss.tm.dto.user.request.ComplianceOfficerRegistrationRequest;
import org.tss.tm.dto.user.response.UserResponse;

import java.util.List;

public interface TenantUserService {
    UserResponse registerComplianceOfficer(ComplianceOfficerRegistrationRequest request, String currentTenantSchema);

    TenantUserResponse getTenantUserBasicDetails(String userEmail);

    List<UserResponse> getAllComplianceOfficer(String userEmail);
}
