package com.ecommerce.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

// TODO: Полный контекстный тест требует внешних сервисов (MongoDB, Redis, Kafka). Включить в CI/CD.
@org.junit.jupiter.api.Disabled("Requires external services (MongoDB, Redis, Kafka) - enable in CI/CD pipeline")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false",
    "spring.config.import="
})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring успешно загружается
    }
}
