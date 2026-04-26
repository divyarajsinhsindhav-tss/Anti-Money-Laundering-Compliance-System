package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.common.response.PagedResponse;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
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

    @GetMapping("/get-tenant-basic-details")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<TenantUserResponse>> getTenantBasicDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest
    ) {
        TenantUserResponse response = tenantUserService.getTenantBasicDetails(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Tenant details fetched successfully",
                httpServletRequest.getRequestURI(),
                response
        ));
    }
}
