package com.ecommerce.integration;

import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для Kafka consumers в Notification Service.
 * Тестирует обработку событий order.created с реальным Kafka брокером.
 * 
 * Требования: Docker должен быть запущен.
 */
@SpringBootTest(classes = com.ecommerce.notification.NotificationServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer(
            DockerImageName.parse("mongo:7")
    );

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationLogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        
        // Отключаем Vault
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
        
        // Redis mock (для rate limiter)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Test
    void testOrderCreatedEventProcessing() throws Exception {
        // Given
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", "ORDER-INT-001");
        event.put("userId", "user-int-001");
        event.put("userEmail", "integration@test.com");
        event.put("totalAmount", 5999.99);
        event.put("timestamp", System.currentTimeMillis());

        String eventJson = objectMapper.writeValueAsString(event);

        // When
        kafkaTemplate.send("order.created", eventJson);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var logs = logRepository.findByEmailAndStatus(
                    "integration@test.com",
                    NotificationLog.NotificationStatus.SENT
            );
            assertFalse(logs.isEmpty());
            assertEquals("order.created", logs.get(0).getEventType());
        });
    }
}
