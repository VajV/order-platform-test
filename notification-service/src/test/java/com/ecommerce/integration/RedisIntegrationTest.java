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

// TODO: Интеграционный тест требует Docker и Testcontainers. Включить после настройки CI/CD.
@org.junit.jupiter.api.Disabled("Requires Docker and Testcontainers - enable in CI/CD pipeline")
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
