package org.tss.tm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.tenant.TenantContext;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateJwtToken(jwt)) {
                String tenantFromJwt = tokenProvider.getTenantFromJwtToken(jwt);
                String tenantFromHeader = request.getHeader(TenantConstants.TENANT_HEADER_NAME);

                if (tenantFromHeader == null || tenantFromHeader.isEmpty()) {
                    tenantFromHeader = TenantConstants.DEFAULT_TENANT;
                }

                if (!tenantFromJwt.equalsIgnoreCase(tenantFromHeader)) {
                    log.error("Tenant mismatch detected! JWT Tenant: {}, Header Tenant: {}", tenantFromJwt,
                            tenantFromHeader);
                    throw new BadCredentialsException("Tenant mismatch. Unauthorized access to tenant resource.");
                }

                String email = tokenProvider.getEmailFromJwtToken(jwt);
                UserDetails userDetails = customUserDetailService.loadUserByUsername(email);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (BadCredentialsException e) {
            log.error("Security violation: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
            return;
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
            resolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
