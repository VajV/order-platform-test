package com.ecommerce.auth.service.token;

import com.ecommerce.auth.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "refresh:";
    private static final String USER_KEY_PREFIX = "user-refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMillis;

    @Override
    public void issue(Long userId, String refreshToken) {
        try {
            String userKey = userKey(userId);
            String oldToken = stringRedisTemplate.opsForValue().get(userKey);
            if (oldToken != null && !oldToken.isBlank()) {
                stringRedisTemplate.delete(tokenKey(oldToken));
            }

            Duration ttl = Duration.ofMillis(refreshTokenExpirationMillis != null ? refreshTokenExpirationMillis : 0L);

            stringRedisTemplate.opsForValue().set(tokenKey(refreshToken), String.valueOf(userId), ttl);
            stringRedisTemplate.opsForValue().set(userKey, refreshToken, ttl);
        } catch (RedisConnectionFailureException e) {
            throw new ServiceUnavailableException("Redis unavailable");
        }
    }

    @Override
    public Optional<Long> getUserId(String refreshToken) {
        try {
            String value = stringRedisTemplate.opsForValue().get(tokenKey(refreshToken));
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(value));
        } catch (RedisConnectionFailureException e) {
            throw new ServiceUnavailableException("Redis unavailable");
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(String refreshToken) {
        try {
            String tokenKey = tokenKey(refreshToken);
            String userIdValue = stringRedisTemplate.opsForValue().get(tokenKey);

            stringRedisTemplate.delete(tokenKey);

            if (userIdValue != null && !userIdValue.isBlank()) {
                String userKey = userKey(Long.parseLong(userIdValue));
                String currentToken = stringRedisTemplate.opsForValue().get(userKey);
                if (refreshToken.equals(currentToken)) {
                    stringRedisTemplate.delete(userKey);
                }
            }
        } catch (RedisConnectionFailureException e) {
            throw new ServiceUnavailableException("Redis unavailable");
        } catch (NumberFormatException ignored) {
        }
    }

    private static String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private static String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
