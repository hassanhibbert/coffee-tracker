package com.hassanhibbert.coffee_tracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real, throwaway Postgres in Docker for tests that need a database. Same engine and
 * version as production, so CHECK constraints, timestamptz and "nulls last" behave
 * exactly as they will in prod (H2 emulates Postgres, and the emulation has gaps).
 *
 * @ServiceConnection points spring.datasource.* at the container automatically.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }
}
