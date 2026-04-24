package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.dto.user.request.ChangePasswordRequest;
import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;
import org.tss.tm.dto.user.response.ChangePasswordResponse;
import org.tss.tm.entity.system.SystemAdmin;
import org.tss.tm.entity.tenant.TenantUser;
import org.tss.tm.repository.SystemAdminRepo;
import org.tss.tm.repository.TenantUserRepo;
import org.tss.tm.security.CustomUserDetails;
import org.tss.tm.security.JwtTokenProvider;
import org.tss.tm.service.interfaces.AuthService;
import org.tss.tm.tenant.TenantContext;
import org.tss.tm.exception.BusinessRuleException;
import org.tss.tm.exception.ResourceNotFoundException;

import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TenantUserRepo tenantUserRepo;
    private final SystemAdminRepo systemAdminRepo;
    private final PasswordEncoder passwordEncoder;

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
            throw new BusinessRuleException("Invalid email or password", "INVALID_CREDENTIALS");
        } catch (Exception e) {
            log.error("Login error for user: {}", loginRequest.getEmail(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ChangePasswordResponse changePassword(ChangePasswordRequest request, String email) {

        log.info("Password change request for user: {}", email);

        String currentTenant = TenantContext.getCurrentTenant();

        if (currentTenant == null) {
            throw new BusinessRuleException("Tenant not resolved", "TENANT_NOT_FOUND");
        }

        try {
            if (TenantConstants.DEFAULT_TENANT.equals(currentTenant)) {

                SystemAdmin sysAdmin = systemAdminRepo.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("SystemAdmin", email));

                validateAndUpdatePassword(
                        sysAdmin.getPasswordHash(),
                        request.getOldPassword(),
                        request.getNewPassword(),
                        sysAdmin::setPasswordHash
                );

                systemAdminRepo.save(sysAdmin);
            }

            else {
                TenantUser tenantUser = tenantUserRepo.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("TenantUser", email));

                validateAndUpdatePassword(
                        tenantUser.getPasswordHash(),
                        request.getOldPassword(),
                        request.getNewPassword(),
                        tenantUser::setPasswordHash
                );

                tenantUserRepo.save(tenantUser);
            }

            return new ChangePasswordResponse("Password changed successfully");

        } catch (Exception e) {
            log.error("Password change failed for user: {}", email, e);
            throw new RuntimeException("Unable to change password");
        }
    }

    private void validateAndUpdatePassword(
            String currentPasswordHash,
            String oldPassword,
            String newPassword,
            Consumer<String> passwordSetter) {

        if (!passwordEncoder.matches(oldPassword, currentPasswordHash)) {
            throw new BusinessRuleException("Old password is incorrect", "INVALID_OLD_PASSWORD");
        }

        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new BusinessRuleException("New password cannot be same as old password", "SAME_PASSWORD");
        }

        passwordSetter.accept(passwordEncoder.encode(newPassword));
    }

}
