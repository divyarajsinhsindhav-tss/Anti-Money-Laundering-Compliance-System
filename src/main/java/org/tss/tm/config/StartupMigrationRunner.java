package org.tss.tm.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.service.interfaces.FlywayMigration;

import javax.sql.DataSource;
import java.util.List;

@Component
public class StartupMigrationRunner {

    private TenantRepo tenantRepository;
    private FlywayMigration flywayMigration;
    public DataSource dataSource;

    public StartupMigrationRunner(TenantRepo tenantRepository, FlywayMigration flywayMigration,
            DataSource dataSource) {
        this.tenantRepository = tenantRepository;
        this.flywayMigration = flywayMigration;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        migratePublicSchema();
        migrateAllTenants();
    }

    public void migrateAllTenants() {
        List<Tenant> schemas = tenantRepository.findAll();

        for (Tenant tenant : schemas) {
            flywayMigration.migrateSchema(tenant.getSchemaName());
        }
    }

    public void migratePublicSchema() {
        Flyway flywayPublic = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .load();

        flywayPublic.migrate();
    }

}
