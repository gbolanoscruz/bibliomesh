package com.bibliomesh;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayAndSchemaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigrationsAndJpaEntitiesMatchPostgreSqlSchema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer migratedVersions = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Integer.class
        );
        assertThat(migratedVersions).isNotNull().isGreaterThanOrEqualTo(2);

        // Verifica que las tablas críticas y columnas nuevas existan
        Integer schoolsCount = jdbc.queryForObject("SELECT COUNT(*) FROM schools", Integer.class);
        assertThat(schoolsCount).isNotNull();
    }
}