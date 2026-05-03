package org.tss.tm.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Component
@Slf4j
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<Object>, HibernatePropertiesCustomizer {
    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

//    @Override
//    public Connection getConnection(Object tenantIdentifier) throws SQLException {
//        log.debug("Switching to schema: {}", tenantIdentifier);
//        final Connection connection = getAnyConnection();
//        connection.setSchema((String) tenantIdentifier);
//        return connection;
//    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        log.debug("Switching to schema: {}", tenantIdentifier);
        final Connection connection = getAnyConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO \"" + tenantIdentifier + "\", public");
        }

        return connection;
    }

//    @Override
//    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
//        log.debug("Releasing connection for schema: {}. Resetting to public.", tenantIdentifier);
//        connection.setSchema("public");
//        releaseAnyConnection(connection);
//    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        log.debug("Releasing connection for schema: {}. Resetting to public.", tenantIdentifier);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        }

        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
