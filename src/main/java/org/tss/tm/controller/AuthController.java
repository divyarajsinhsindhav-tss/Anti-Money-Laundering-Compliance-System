package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;
import org.tss.tm.service.interfaces.AuthService;
import org.tss.tm.tenant.TenantContext;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {

        private final AuthService authService;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest loginRequest,
                        HttpServletRequest httpServletRequest) {
                System.out.println(TenantContext.getCurrentTenant());
                AuthResponse authResponse = authService.login(loginRequest);
                return ResponseEntity.ok(ApiResponse.of(
                                HttpStatus.OK,
                                authResponse.getMessage(),
                                httpServletRequest.getRequestURI(),
                                authResponse));
        }

}
