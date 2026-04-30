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
import org.tss.tm.dto.admin.request.AssignScenarioRequest;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.service.interfaces.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
public class SystemAdminController {

        private final AdminService adminService;

        @PostMapping("/assign-scenario")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<Void>> assignScenario(
                        @Valid @RequestBody AssignScenarioRequest assignScenarioRequest,
                        HttpServletRequest httpServletRequest) {
                adminService.assignScenario(assignScenarioRequest);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Scenario assigned to tenant successfully",
                                httpServletRequest.getRequestURI(),
                                null));
        }

        @GetMapping("/scenarios")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<Page<ScenarioResponse>>> getScenarios(
                        @PageableDefault(size = 10) Pageable pageable,
                        HttpServletRequest httpServletRequest) {
                Page<ScenarioResponse> scenarios = adminService.getScenarios(pageable);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Scenarios fetched successfully",
                                httpServletRequest.getRequestURI(),
                                scenarios));
        }

        @GetMapping("/scenario/{code}")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<ScenarioDetailResponse>> getScenario(
                        @PathVariable String code,
                        HttpServletRequest httpServletRequest) {
                ScenarioDetailResponse scenario = adminService.getScenario(code);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Scenario details fetched successfully",
                                httpServletRequest.getRequestURI(),
                                scenario));
        }

        @GetMapping("/dashboard/stats")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<org.tss.tm.dto.admin.response.DashboardStatsResponse>> getDashboardStats(
                        HttpServletRequest httpServletRequest) {
                org.tss.tm.dto.admin.response.DashboardStatsResponse stats = adminService.getDashboardStats();
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Dashboard statistics fetched successfully",
                                httpServletRequest.getRequestURI(),
                                stats));
        }

        @GetMapping("/job-records")
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public ResponseEntity<ApiResponse<Page<org.tss.tm.dto.admin.response.DashboardStatsResponse.RecentJobResponse>>> getJobRecords(
                        @RequestParam(required = false) org.tss.tm.common.enums.JobStatus status,
                        @RequestParam(required = false) String tenantCode,
                        @PageableDefault(size = 10) Pageable pageable,
                        HttpServletRequest httpServletRequest) {
                Page<org.tss.tm.dto.admin.response.DashboardStatsResponse.RecentJobResponse> jobRecords = adminService
                                .getJobRecords(status, tenantCode, pageable);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                "Job records fetched successfully",
                                httpServletRequest.getRequestURI(),
                                jobRecords));
        }

}
