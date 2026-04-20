package org.tss.tm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.tss.tm.service.interfaces.FlywayMigration;

import javax.sql.DataSource;

@Service
@Slf4j
public class FlywayMigrationImpl implements FlywayMigration {

    @Autowired
    private DataSource dataSource;

    @Override
    public void migrateSchema(String schema) {

        Flyway flywayTenant = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flywayTenant.migrate();

    }

    @Async
    @Override
    public void migrateSchemaAsync(String schema) {
        try {
            log.info("Migrating Flyway schema {}", schema);
            migrateSchema(schema);
            log.info("Migrated Flyway schema {}", schema);
        } catch (Exception e) {
            log.error("Failed to Migrate Flyway schema {}", schema, e);
            e.printStackTrace();
        }
    }
}
