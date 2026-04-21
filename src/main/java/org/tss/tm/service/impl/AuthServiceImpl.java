package org.tss.tm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;
import org.tss.tm.security.CustomUserDetails;
import org.tss.tm.security.JwtTokenProvider;
import org.tss.tm.service.interfaces.AuthService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Processing login request for user: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("Login successful for user: {} with role: {}", loginRequest.getEmail(), role);

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .role(role)
                .message("Authentication successful")
                .build();
    }
}

