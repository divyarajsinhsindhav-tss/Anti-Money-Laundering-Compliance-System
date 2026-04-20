package org.tss.tm.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tss.tm.service.interfaces.FlywayMigration;

import javax.sql.DataSource;
import java.util.List;

@Component
public class StartupMigrationRunner {

    private TenantRepository tenantRepository;
    private FlywayMigration flywayMigration;
    public DataSource dataSource;

    public StartupMigrationRunner(TenantRepository tenantRepository, FlywayMigration flywayMigration,
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
        List<String> schemas = tenantRepository.findAllSchemas();

        for (String schema : schemas) {
            flywayMigration.migrateSchema(schema);
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
