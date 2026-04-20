package org.tss.tm.service.interfaces;

public interface FlywayMigration {
    void migrateSchema(String schema);
    void migrateSchemaAsync(String schema);
}
