package com.ecommerce.integration;

import com.ecommerce.notification.service.RedisRateLimiter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для Redis Rate Limiter в Notification Service.
 * Использует embedded Redis (Docker не требуется).
 */
@SpringBootTest(classes = com.ecommerce.notification.NotificationServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"order.created"})
@ActiveProfiles("test")
class RedisIntegrationTest {

    private static RedisServer redisServer;

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeAll
    static void startRedis() throws Exception {
        redisServer = RedisServer.newRedisServer()
                .port(6379)
                .build();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws Exception {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                connection.serverCommands().flushAll();
                return null;
            });
        } catch (Exception e) {
            // Ignore if Redis not available
        }
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
