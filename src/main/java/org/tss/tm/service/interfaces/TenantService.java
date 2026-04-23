package org.tss.tm.service.interfaces;

import org.tss.tm.entity.system.Tenant;

import java.sql.SQLException;

public interface TenantService {
    void createTenant(String tenantId) throws SQLException;
    Tenant getTenantByName(String tenantName);
    void migrateAllTenants();
    Tenant getCurrentTenant();
}

