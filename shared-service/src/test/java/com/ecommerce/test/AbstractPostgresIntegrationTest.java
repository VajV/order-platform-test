package com.ecommerce.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Базовый класс для интеграционных тестов с PostgreSQL.
 * Использует Testcontainers для запуска реальной БД в Docker.
 * 
 * Использование:
 * <pre>
 * {@code
 * @SpringBootTest
 * class MyIntegrationTest extends AbstractPostgresIntegrationTest {
 *     @Test
 *     void testSomething() {
 *         // тест с реальной PostgreSQL
 *     }
 * }
 * }
 * </pre>
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Переиспользование контейнера между тестами

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        
        // Отключаем Vault
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
    }
}

