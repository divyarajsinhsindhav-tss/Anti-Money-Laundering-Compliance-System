package org.tss.tm.service.impl;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.service.interfaces.FlywayMigration;
import org.tss.tm.service.interfaces.TenantService;
import org.tss.tm.tenant.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

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

        Tenant tenant = Tenant.builder()
                .name(tenantId)
                .displayName(tenantId)
                .schemaName(schemaName)
                .build();

        tenantRepo.save(tenant);

        flywayMigration.migrateSchema(schemaName);

    }

    @Override
    public Tenant getTenantByName(String tenantName) {
        return tenantRepo.findByName(tenantName)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantName));
    }

    @Override
    public void migrateAllTenants() {
        tenantRepo.findAll().forEach(tenant -> {
            flywayMigration.migrateSchema(tenant.getSchemaName());
        });
    }

    private void validateTenantName(String tenantName) {

        if (tenantName == null) {
            throw new IllegalArgumentException("tenantName is null");
        }
        if (!tenantName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("tenantName contains invalid schema name");
        }
    }

    @Override
    public Tenant getCurrentTenant() {
        Tenant currentTenant=tenantRepo
                .findByTenantCode(TenantContext.getCurrentTenant())
                .orElseThrow(()->new RuntimeException("Tenant not found."));

        return currentTenant;
    }
}
