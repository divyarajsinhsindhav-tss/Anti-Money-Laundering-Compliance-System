package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.tss.tm.common.enums.CaseStatus;
import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.dto.tenant.response.CaseResponse;
import org.tss.tm.dto.tenant.response.CreateCaseResponse;
import org.tss.tm.dto.tenant.response.CaseDetailResponse;
import org.tss.tm.entity.tenant.AmlCase;
import org.tss.tm.service.interfaces.CaseService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

        private final CaseService caseService;

        @PostMapping
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<CreateCaseResponse>> createCase(
                        @Valid @RequestBody CreateCaseRequest request,
                        @AuthenticationPrincipal UserDetails userDetails,
                        HttpServletRequest httpServletRequest) {
                log.info("Received request to create case from user: {}", userDetails.getUsername());
                AmlCase amlCase = caseService.createCase(request, userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                                HttpStatus.CREATED,
                                "Case created successfully with code: " + amlCase.getCaseCode(),
                                httpServletRequest.getRequestURI(),
                                CreateCaseResponse.builder()
                                                .caseCode(amlCase.getCaseCode())
                                                .build()));
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<PagedResponse<CaseResponse>>> getAllCases(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(required = false) CaseStatus status,
                        Pageable pageable,
                        HttpServletRequest httpServletRequest) {
                log.info("Received request to get all cases with status: {} from user: {}", status,
                                userDetails.getUsername());

                boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_BANK_ADMIN"));

                Page<CaseResponse> cases = caseService.getAllCases(status, userDetails.getUsername(), isAdmin,
                                pageable);

                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Cases retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                PagedResponse.of(cases.getContent(), cases.getNumber(), cases.getSize(),
                                                cases.getTotalElements(),
                                                cases.getSort().toString(),
                                                cases.getSort().isSorted()
                                                                ? cases.getSort().iterator().next().getDirection()
                                                                                .name()
                                                                : "ASC")));
        }

        @GetMapping("/{caseCode}")
        @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
        public ResponseEntity<ApiResponse<CaseDetailResponse>> getCase(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable String caseCode,
                        HttpServletRequest httpServletRequest) {
                log.info("Received request to get case details for case code: {} from user: {}", caseCode,
                                userDetails.getUsername());

                boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_BANK_ADMIN"));

                CaseDetailResponse caseDetail = caseService.getCase(caseCode, userDetails.getUsername(), isAdmin);

                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Case details retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                caseDetail));
        }

        @PostMapping("/auto-generate")
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<List<CaseResponse>>> autoGenerateCases(
                        @AuthenticationPrincipal UserDetails userDetails,
                        HttpServletRequest httpServletRequest) {
                log.info("Received request to auto-generate cases from user: {}", userDetails.getUsername());
                List<CaseResponse> cases = caseService.autoGenerateCases(userDetails.getUsername());
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Auto-generated " + cases.size() + " cases successfully",
                                httpServletRequest.getRequestURI(),
                                cases));
        }

    @PatchMapping("/{caseCode}/assign")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponse>> assignCase(
            @PathVariable String caseCode,
            @RequestParam String assignedToUserCode,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received request to assign case {} to user code {}", caseCode, assignedToUserCode);
        CaseResponse caseResponse = caseService.assignCase(caseCode, assignedToUserCode);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Case assigned successfully",
                httpServletRequest.getRequestURI(),
                caseResponse
        ));
    }
}
