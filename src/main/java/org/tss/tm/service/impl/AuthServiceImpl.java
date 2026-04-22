package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.security.CustomUserDetails;
import org.tss.tm.security.JwtTokenProvider;
import org.tss.tm.service.interfaces.AuthService;
import org.tss.tm.tenant.TenantContext;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TenantUserRepo tenantUserRepo;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Processing login request for user: {}", loginRequest.getEmail());
        String currentTenant = TenantContext.getCurrentTenant();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateJwtToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            // Updated last login for tenant users
            if (!"public".equals(currentTenant)) {
                tenantUserRepo.findByEmail(loginRequest.getEmail()).ifPresent(user -> {
                    user.setLastLogin(java.time.LocalDateTime.now());
                    tenantUserRepo.save(user);
                });
            }

            log.info("Login successful for user: {} with role: {}", loginRequest.getEmail(), role);

            return AuthResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .role(role)
                    .message("Authentication successful")
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} - Bad Credentials", loginRequest.getEmail());
            throw new RuntimeException("Invalid email or password");
        } catch (Exception e) {
            log.error("Login error for user: {}", loginRequest.getEmail(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

}
