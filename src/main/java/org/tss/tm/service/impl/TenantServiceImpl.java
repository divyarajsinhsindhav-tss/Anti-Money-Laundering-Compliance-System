package org.tss.tm.service.impl;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.service.interfaces.FlywayMigration;
import org.tss.tm.service.interfaces.TenantService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class TenantServiceImpl implements TenantService {

    private DataSource dataSource;
    private FlywayMigration flywayMigration;
    private TenantRepo tenantRepo;

    public TenantServiceImpl(DataSource dataSource, FlywayMigration flywayMigration, TenantRepo tenantRepo) {
        this.dataSource = dataSource;
        this.flywayMigration = flywayMigration;
        this.tenantRepo = tenantRepo;
    }

    @Override
    @Transactional
    public void createTenant(String tenantId) throws SQLException {
        String schemaName = "tenant_" + tenantId;
        validateTenantName(schemaName);

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        }

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSchemaName(schemaName);

        tenantRepo.save(tenant);

        flywayMigration.migrateSchema(schemaName);

    }

    @Override
    public Tenant getTenantByName(String tenantName) {
        Tenant tenant = tenantRepo.findByName(tenantName)
                .orElseThrow(() -> {
                    throw new ChangeSetPersister.NotFoundException("Tenant not found");
                });
        return tenant;
    }

    private void validateTenantName(String tenantName) {
        if (tenantName == null) {
            throw new IllegalArgumentException("tenantName is null");
        }
        if (!tenantName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("tenantName contains invalid schema name");
        }
    }
}
