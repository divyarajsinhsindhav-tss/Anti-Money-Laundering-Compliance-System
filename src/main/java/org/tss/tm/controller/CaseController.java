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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.tenant.request.CreateCaseRequest;
import org.tss.tm.entity.tenant.AmlCase;
import org.tss.tm.service.interfaces.CaseService;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

        private final CaseService caseService;

        @PostMapping
        @PreAuthorize("hasRole('BANK_ADMIN')")
        public ResponseEntity<ApiResponse<UUID>> createCase(
                @Valid @RequestBody CreateCaseRequest request,
                @AuthenticationPrincipal UserDetails userDetails,
                HttpServletRequest httpServletRequest
        ) {
                log.info("Received request to create case from user: {}", userDetails.getUsername());
                AmlCase amlCase = caseService.createCase(request, userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Case created successfully with code: " + amlCase.getCaseCode(),
                        httpServletRequest.getRequestURI(),
                        amlCase.getCaseId()
                ));
        }

}
