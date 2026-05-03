package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.common.response.PagedResponse;
import org.tss.tm.dto.tenant.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.service.interfaces.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.sql.SQLException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

        private final TenantService tenantService;

        @GetMapping("/transaction-stats")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<TransactionDashboardResponse>> getTransactionStats(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch transaction dashboard stats");
                TransactionDashboardResponse response = tenantService.getTransactionDashboardStats();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Transaction stats fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/transaction-jobs")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<List<TransactionDashboardResponse.RecentJobResponse>>> getTransactionJobs(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch recent transaction jobs");
                List<TransactionDashboardResponse.RecentJobResponse> response = tenantService.getTransactionJobs();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Transaction jobs fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/customer-stats")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getCustomerStats(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch customer dashboard stats");
                CustomerDashboardResponse response = tenantService.getCustomerDashboardStats();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Customer stats fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/customer-jobs")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<List<CustomerDashboardResponse.RecentJobResponse>>> getCustomerJobs(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch recent customer jobs");
                List<CustomerDashboardResponse.RecentJobResponse> response = tenantService.getCustomerJobs();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Customer jobs fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/customer-errors")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<Page<FileErrorResponse>>> getCustomerErrors(
                        @RequestParam(required = false) String jobId,
                        @PageableDefault(size = 10) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch customer errors");
                Page<FileErrorResponse> errorResponse = tenantService.getCustomerErrors(jobId, pageable);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Customer errors fetched successfully",
                                httpServletRequest.getRequestURI(),
                                errorResponse
                ));
        }

        @GetMapping("/rule-engine-stats")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getRuleEngineStats(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch rule engine stats");
                java.util.Map<String, Long> stats = tenantService.getRuleEngineStats();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Rule engine stats fetched successfully",
                                httpServletRequest.getRequestURI(),
                                stats
                ));
        }

        @GetMapping("/rule-engine-jobs")
        @PreAuthorize("hasAnyRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<PagedResponse<RuleJobResponse.RecentJobResponse>>> getRuleEngineJobs(
                        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch recent rule engine jobs");
                Page<RuleJobResponse.RecentJobResponse> response = tenantService.getRuleEngineJobs(pageable);
                
                PagedResponse<RuleJobResponse.RecentJobResponse> pagedResponse = PagedResponse.of(
                        response.getContent(),
                        response.getNumber(),
                        response.getSize(),
                        response.getTotalElements(),
                        pageable.getSort().toString(),
                        pageable.getSort().isSorted() 
                            ? pageable.getSort().iterator().next().getDirection().name() 
                            : "DESC"
                );

                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Rule engine jobs fetched successfully",
                                httpServletRequest.getRequestURI(),
                                pagedResponse
                ));
        }

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

    @GetMapping("/transaction-errors")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<Page<FileErrorResponse>>> getTransactionErrors(
            @RequestParam(required = false) String jobId,
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received request to fetch transaction errors");
        Page<FileErrorResponse> errorResponse = tenantService.getTransactionErrors(jobId, pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Transaction errors fetched successfully",
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

        @GetMapping("/customers")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<PagedResponse<CustomerListResponse>>> getAllCustomers(
                        @RequestParam(required = false) String search,
                        @PageableDefault(size = 20) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch all customers with search: {}", search);
                Page<CustomerListResponse> response = tenantService.getAllCustomers(search, pageable);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Customers fetched successfully",
                                httpServletRequest.getRequestURI(),
                                PagedResponse.of(
                                                response.getContent(),
                                                response.getNumber(),
                                                response.getSize(),
                                                response.getTotalElements(),
                                                pageable.getSort().toString(),
                                                pageable.getSort().isSorted() ? pageable.getSort().iterator().next().getDirection().name() : "ASC"
                                )
                ));
        }

        @GetMapping("/customers/{customerId}")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(
                        @PathVariable UUID customerId,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch customer detail for ID: {}", customerId);
                CustomerDetailResponse response = tenantService.getCustomerDetail(customerId);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Customer detail fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/transactions")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<PagedResponse<TransactionListResponse>>> getAllTransactions(
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) LocalDateTime startDate,
                        @RequestParam(required = false) LocalDateTime endDate,
                        @PageableDefault(size = 20, sort = "txnTimestamp", direction = Sort.Direction.DESC) Pageable pageable,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch all transactions with search: {}, range: {} to {}", search, startDate, endDate);
                Page<TransactionListResponse> response = tenantService.getAllTransactions(search, startDate, endDate, pageable);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Transactions fetched successfully",
                                httpServletRequest.getRequestURI(),
                                PagedResponse.of(
                                                response.getContent(),
                                                response.getNumber(),
                                                response.getSize(),
                                                response.getTotalElements(),
                                                pageable.getSort().toString(),
                                                pageable.getSort().isSorted() ? pageable.getSort().iterator().next().getDirection().name() : "DESC"
                                )
                ));
        }

        @GetMapping("/transactions/{transactionId}")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<TransactionDetailResponse>> getTransactionDetail(
                        @PathVariable UUID transactionId,
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch transaction detail for ID: {}", transactionId);
                TransactionDetailResponse response = tenantService.getTransactionDetail(transactionId);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Transaction detail fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }

        @GetMapping("/dashboard-stats")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<TenantDashboardResponse>> getDashboardStats(
                        HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to fetch unified dashboard stats");
                TenantDashboardResponse response = tenantService.getDashboardStats();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Dashboard stats fetched successfully",
                                httpServletRequest.getRequestURI(),
                                response
                ));
        }
}
