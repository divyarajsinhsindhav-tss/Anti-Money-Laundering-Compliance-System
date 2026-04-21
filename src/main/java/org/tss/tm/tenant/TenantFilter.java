package org.tss.tm.tenant;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.tss.tm.common.constant.TenantConstants;
import org.tss.tm.security.JwtTokenProvider;

import java.io.IOException;

@Component
@Slf4j
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantName = req.getHeader(TenantConstants.TENANT_HEADER_NAME);

        if (tenantName == null || tenantName.isEmpty()) {
            tenantName = TenantConstants.DEFAULT_TENANT;
        }

        TenantContext.setCurrentTenant(tenantName);
        log.info("Request intercepted for tenant: {}", tenantName);

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
