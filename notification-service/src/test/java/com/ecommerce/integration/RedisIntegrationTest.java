package com.ecommerce.integration;

import com.ecommerce.notification.service.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для Redis Rate Limiter в Notification Service.
 * Тестирует механизм ограничения частоты запросов.
 * 
 * Требования: Docker должен быть запущен.
 */
@SpringBootTest(classes = com.ecommerce.notification.NotificationServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // Отключаем Vault и внешние зависимости
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/test");
    }

    @BeforeEach
    void setUp() {
        // ✅ Исправлено: используем execute вместо getConnection
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    void testRateLimiterAllowsRequestsUnderLimit() {
        String email = "test@example.com";

        // First 5 requests should be allowed (limit is 5 in test config)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest(email), "Request " + (i+1) + " should be allowed");
        }

        // 6th request should be blocked
        assertFalse(rateLimiter.allowRequest(email), "Request 6 should be blocked");
    }

    @Test
    void testRateLimiterReset() {
        String email = "reset@example.com";

        // Exhaust limit
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowRequest(email);
        }
        assertFalse(rateLimiter.allowRequest(email));

        // Reset
        rateLimiter.resetLimit(email);

        // Should allow again
        assertTrue(rateLimiter.allowRequest(email));
    }

    @Test
    void testGetRemainingRequests() {
        String email = "remaining@example.com";

        assertEquals(5, rateLimiter.getRemainingRequests(email));

        rateLimiter.allowRequest(email);
        assertEquals(4, rateLimiter.getRemainingRequests(email));

        rateLimiter.allowRequest(email);
        rateLimiter.allowRequest(email);
        assertEquals(2, rateLimiter.getRemainingRequests(email));
    }
}
