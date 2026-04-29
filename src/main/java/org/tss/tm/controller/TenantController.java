package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.common.response.PagedResponse;
import org.tss.tm.dto.tenant.response.ScenarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.TenantAvailableResponse;
import org.tss.tm.dto.tenant.response.FileErrorResponse;
import org.tss.tm.dto.tenant.response.TenantDetailResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
import org.tss.tm.service.interfaces.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.sql.SQLException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

        private final TenantService tenantService;

        @PostMapping("/register")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<TenantResponse>> registerTenant(
                        @Valid @RequestBody TenantRegistrationRequest request,
                        @AuthenticationPrincipal UserDetails userDetails,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to register tenant: {} from user: {}", request.getName(),
                                userDetails.getUsername());
                TenantResponse response = tenantService.createTenant(request, userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Tenant registered successfully",
                        httpServletRequest.getRequestURI(),
                        response
                    ));
        }

        @GetMapping("/check-tenant-available")
        public ResponseEntity<ApiResponse<TenantAvailableResponse>> checkTenantAvailable(
                        @RequestParam("tenantCode") String tenantCode,
                        HttpServletRequest httpServletRequest
        ) {
                TenantAvailableResponse response = tenantService.tenantAvailable(tenantCode);
                return ResponseEntity.ok(ApiResponse.of(
                        HttpStatus.OK,
                        "Tenant availablity checked successfully.",
                        httpServletRequest.getRequestURI(),
                        response
                ));
        }

        @GetMapping
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch all tenants");
                List<TenantResponse> response = tenantService.getAllTenants();
                return ResponseEntity.ok(ApiResponse.of(
                        HttpStatus.OK,
                        "Tenants fetched successfully",
                        httpServletRequest.getRequestURI(),
                        response
                ));
        }

        @GetMapping("/scenarios")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<PagedResponse<ScenarioResponse>>> getScenarios(
                        @PageableDefault(size = 10) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                Page<ScenarioResponse> scenarios = tenantService.getScenarios(pageable);

                PagedResponse<ScenarioResponse> pageResponse = PagedResponse.of(
                        scenarios.getContent(),
                        scenarios.getNumber(),
                        scenarios.getSize(),
                        scenarios.getTotalElements(),
                        pageable.getSort().toString(),
                        pageable.getSort().isSorted()
                                        ? pageable.getSort().iterator().next().getDirection().name()
                                        : "ASC"
                );
                return ResponseEntity.ok(ApiResponse.of(
                        HttpStatus.OK,
                        "Tenant purchesed history fetched successfully",
                        httpServletRequest.getRequestURI(),
                        pageResponse
                ));
        }

        @GetMapping("/file-error")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<Page<FileErrorResponse>>> getFileError(
                        @PageableDefault(size = 10) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch file errors");
                Page<FileErrorResponse> errorResponse = tenantService.getFileError(pageable);
                return ResponseEntity.ok(ApiResponse.of(
                        HttpStatus.OK,
                        "File errors fetched successfully",
                        httpServletRequest.getRequestURI(),
                        errorResponse
                ));
        }

        @GetMapping("/{tenantCode}")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'SYSTEM_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<TenantDetailResponse>> getTenant(
                        @PathVariable String tenantCode,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch tenant details for code: {}", tenantCode);
                TenantDetailResponse response = tenantService.getTenantDetail(tenantCode);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Tenant details fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

}
