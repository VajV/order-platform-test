package com.ecommerce.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${notification.rate-limit.max-per-hour:10}")
    private int maxPerHour;

    @Value("${notification.rate-limit.window-seconds:3600}")
    private long windowSeconds;

    private static final String KEY_PREFIX = "rate_limit:notification:";

    /**
     * Leaky bucket rate limiter
     * Возвращает true если можно отправить, false если лимит превышен
     */
    public boolean allowRequest(String email) {
        String key = KEY_PREFIX + email;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                currentCount = 0L;
            }

            // Первый запрос - устанавливаем TTL
            if (currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (currentCount > maxPerHour) {
                log.warn("⚠️ Rate limit exceeded for email: {}, count: {}/{}",
                        email, currentCount, maxPerHour);
                return false;
            }

            log.debug("✅ Rate limit check passed for email: {}, requests: {}/{}",
                    email, currentCount, maxPerHour);
            return true;

        } catch (Exception e) {
            log.error("❌ Error checking rate limit for email: {}", email, e);
            // Fail-open: при ошибке Redis пропускаем запрос
            return true;
        }
    }

    /**
     * Получить оставшееся количество запросов
     */
    public long getRemainingRequests(String email) {
        String key = KEY_PREFIX + email;

        try {
            String countStr = (String) redisTemplate.opsForValue().get(key);
            long currentCount = countStr != null ? Long.parseLong(countStr) : 0L;
            return Math.max(0, maxPerHour - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining requests for: {}", email, e);
            return maxPerHour;
        }
    }

    /**
     * Сбросить лимит для email (для тестирования)
     */
    public void resetLimit(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Rate limit reset for: {}", email);
    }
}
