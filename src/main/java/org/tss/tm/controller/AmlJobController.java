package org.tss.tm.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.tenant.response.RuleEngineResponse;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.service.interfaces.AmlJobService;
import org.tss.tm.service.interfaces.JobService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aml-job")
@RequiredArgsConstructor
public class AmlJobController {

    private final AmlJobService amlJobService;
    private final JobService jobService;

    @PostMapping("/execute")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<ApiResponse<RuleEngineResponse>> triggerManualJob(
            HttpServletRequest request
    ) {

        RuleEngineResponse response=amlJobService.executeTenantScenarios();

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Job Started Successfully",
                request.getRequestURI(),
                response
        ));
    }
}