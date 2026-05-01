package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.tss.tm.common.enums.AlertStatus;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.common.response.PagedResponse;
import org.tss.tm.dto.tenant.response.AlertDetailResponse;
import org.tss.tm.dto.tenant.response.AlertResponse;
import org.tss.tm.service.interfaces.AlertService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.dto.tenant.request.UpdateAlertStatusRequest;

import jakarta.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<PagedResponse<AlertResponse>>> getAllAlerts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String alertCode,
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received request to fetch all alerts with alertCode: {} and status: {} by user: {}", alertCode, status, userDetails.getUsername());
        Page<AlertResponse> alerts = alertService.getAllAlerts(userDetails.getUsername(), alertCode, status, pageable);

        PagedResponse<AlertResponse> pagedResponse = PagedResponse.of(
                alerts.getContent(),
                alerts.getNumber(),
                alerts.getSize(),
                alerts.getTotalElements(),
                pageable.getSort().toString(),
                pageable.getSort().isSorted() ? pageable.getSort().iterator().next().getDirection().name() : "ASC");

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Alerts fetched successfully",
                httpServletRequest.getRequestURI(),
                pagedResponse
        ));
    }

    @PatchMapping("/{alertCode}/status")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<AlertResponse>> updateAlertStatus(
            @PathVariable String alertCode,
            @Valid @RequestBody UpdateAlertStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received request to update alert status for alert: {} to {} by user: {}", alertCode,
                request.getStatus(), userDetails.getUsername());
        AlertResponse response = alertService.updateAlertStatus(alertCode, request.getStatus(), request.getReason(),
                userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Alert status updated successfully",
                httpServletRequest.getRequestURI(),
                response
        ));
    }

    @GetMapping("/{alertCode}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<AlertDetailResponse>> getAlert(
            @AuthenticationPrincipal UserDetails userDetail,
            @PathVariable String alertCode,
            HttpServletRequest httpServletRequest
    ) {
        AlertDetailResponse response = alertService.getAlert(userDetail.getUsername(), alertCode);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Alert details fetched successfully",
                httpServletRequest.getRequestURI(),
                response
        ));
    }
}
