package org.tss.tm.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.service.interfaces.AmlJobService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aml-job")
@RequiredArgsConstructor
public class AmlJobController {

    private final AmlJobService amlJobService;

    @PostMapping("/execute")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<String> triggerManualJob(
            @RequestParam(defaultValue = "30") int adminDays) {

        UUID currentJobId = UUID.randomUUID();

        amlJobService.executeTenantScenarios(adminDays);

        return ResponseEntity.ok("AML Scenarios executed successfully for Job ID: " + currentJobId);
    }
}