package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.entity.system.Tenant;

import java.util.List;
import java.sql.SQLException;

import org.springframework.security.core.userdetails.UserDetails;

public interface TenantService {
    TenantResponse createTenant(TenantRegistrationRequest request, String email) throws SQLException;

    Tenant getTenantByName(String tenantName);

    List<TenantResponse> getAllTenants();

    void migrateAllTenants();
}

