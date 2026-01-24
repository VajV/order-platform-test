package com.ecommerce.service;

import com.ecommerce.notification.service.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Lenient stubbing to avoid UnnecessaryStubbingException in tests that don't use opsForValue
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(rateLimiter, "maxPerHour", 10);
        ReflectionTestUtils.setField(rateLimiter, "windowSeconds", 3600L);
    }

    @Test
    void testAllowRequest_FirstRequest() {
        // Given
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        boolean result = rateLimiter.allowRequest(email);

        // Then
        assertTrue(result);
        verify(valueOperations).increment(contains(email));
        verify(redisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    void testAllowRequest_UnderLimit() {
        // Given
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When
        boolean result = rateLimiter.allowRequest(email);

        // Then
        assertTrue(result);
    }

    @Test
    void testAllowRequest_OverLimit() {
        // Given
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(11L);

        // When
        boolean result = rateLimiter.allowRequest(email);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetRemainingRequests() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn("3");

        // When
        long remaining = rateLimiter.getRemainingRequests(email);

        // Then
        assertEquals(7, remaining);
    }

    @Test
    void testResetLimit() {
        // Given
        String email = "test@example.com";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        rateLimiter.resetLimit(email);

        // Then
        verify(redisTemplate).delete(contains(email));
    }
}
