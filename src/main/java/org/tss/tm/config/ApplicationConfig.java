package org.tss.tm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tss.tm.security.CustomUserDetailsService;
import org.tss.tm.tenant.TenantContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final DataSource dataSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(customUserDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * NamedParameterJdbcTemplate wired to a tenant-aware DataSource proxy.
     *
     * IMPORTANT: SchemaMultiTenantConnectionProvider only works for Hibernate/JPA.
     * NamedParameterJdbcTemplate bypasses it and talks to the DataSource directly.
     * This proxy ensures the PostgreSQL 'search_path' is set for all raw SQL queries.
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        DataSource tenantAwareDataSource = new DelegatingDataSource(dataSource) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection conn = super.getConnection();
                applySearchPath(conn);
                return conn;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                Connection conn = super.getConnection(username, password);
                applySearchPath(conn);
                return conn;
            }

            private void applySearchPath(Connection conn) throws SQLException {
                String tenantId = TenantContext.getCurrentTenant();
                if (tenantId != null && !tenantId.isBlank()) {
                    try (var stmt = conn.createStatement()) {
                        stmt.execute("SET search_path TO \"" + tenantId + "\", public");
                    }
                }
            }
        };
        return new NamedParameterJdbcTemplate(tenantAwareDataSource);
    }
}
