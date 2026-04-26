package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.TenantAvailableResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
import org.tss.tm.entity.system.Tenant;

import java.util.List;
import java.sql.SQLException;

public interface TenantService {
    TenantResponse createTenant(TenantRegistrationRequest request, String email);

    TenantAvailableResponse tenantAvailable(String tenantCode);


    Tenant getTenantByName(String tenantName);

    List<TenantResponse> getAllTenants();

    void migrateAllTenants();
    Tenant getCurrentTenant();

    String getTenantCodeBySchemaName(String schemaName);
}

