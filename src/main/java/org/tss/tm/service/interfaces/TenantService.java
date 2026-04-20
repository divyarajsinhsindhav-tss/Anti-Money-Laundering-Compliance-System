package org.tss.tm.service.interfaces;

import java.sql.SQLException;

public interface TenantService {
    void createTenant(String tenantId) throws SQLException;

    Tenant getTenantByName(String tenantName);
}
