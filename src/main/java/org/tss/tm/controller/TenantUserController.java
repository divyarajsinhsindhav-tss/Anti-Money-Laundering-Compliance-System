package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.user.request.ComplianceOfficerRegistrationRequest;
import org.tss.tm.dto.user.response.UserResponse;
import org.tss.tm.service.interfaces.TenantUserService;
import org.tss.tm.tenant.TenantContext;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tenant-users")
public class TenantUserController {

    private final TenantUserService tenantUserService;

    @PostMapping("/compliance-officer")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> registerComplianceOfficer(
            @Valid @RequestBody ComplianceOfficerRegistrationRequest request,
            HttpServletRequest httpServletRequest) {
        
        String currentTenant = TenantContext.getCurrentTenant();
        log.info("Bank Admin registering compliance officer for tenant: {}", currentTenant);
        
        UserResponse response = tenantUserService.registerComplianceOfficer(request, currentTenant);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                HttpStatus.CREATED,
                "Compliance officer registered successfully",
                httpServletRequest.getRequestURI(),
                response));
    }
}
