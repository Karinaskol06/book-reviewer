package com.project.bookreviewer.infrastructure.persistence.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared Postgres container for {@code @DataJpaTest} integration tests.
 * <p>
 * Interview notes:
 * <ul>
 *   <li>{@code @Container} — Testcontainers starts/stops this Docker Postgres for the test class</li>
 *   <li>{@code @DynamicPropertySource} — wires Spring datasource to the container's JDBC URL</li>
 *   <li>Callers must use {@code @AutoConfigureTestDatabase(replace = NONE)} so Boot does not swap in H2</li>
 * </ul>
 */
@Testcontainers
public abstract class AbstractPostgresDataJpaTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookreviewer_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
