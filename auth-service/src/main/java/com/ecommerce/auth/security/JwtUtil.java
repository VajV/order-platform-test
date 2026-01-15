package com.ecommerce.auth.security;

import com.ecommerce.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Утилита для работы с JWT токенами.
 *
 * Допущения (учебный проект):
 * - Используем симметричный алгоритм HS256.
 * - Секрет лежит в конфиге/переменных окружения.
 * - Для сдачи ТЗ этого достаточно, т.к. auth-service – единственная точка выдачи токенов.
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationMillis;

    /**
     * Генерация access token для User.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole().name());
        // tokenType по умолчанию "access" – можно не писать, но для ясности можно добавить
        // claims.put(CLAIM_TOKEN_TYPE, "access");

        return createToken(claims, user.getUsername(), accessTokenExpirationMillis);
    }

    /**
     * Генерация refresh token для User.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);

        return createToken(claims, user.getUsername(), refreshTokenExpirationMillis);
    }

    /**
     * Генерация access token для UserDetails (на всякий случай).
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User user) {
            claims.put(CLAIM_USER_ID, user.getId());
            claims.put(CLAIM_EMAIL, user.getEmail());
            claims.put(CLAIM_ROLE, user.getRole().name());
        }
        return createToken(claims, userDetails.getUsername(), accessTokenExpirationMillis);
    }

    /**
     * Базовый метод создания JWT.
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationMillis) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis != null ? expirationMillis : 0L);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Извлечение username (subject) из токена.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Извлечение userId из токена (если присутствует).
     */
    public Optional<String> extractUserId(String token) {
        return Optional.ofNullable(extractClaim(token, claims -> {
            Object value = claims.get(CLAIM_USER_ID);
            return value != null ? value.toString() : null;
        }));
    }

    /**
     * Извлечение роли из токена (если присутствует).
     */
    public Optional<String> extractRole(String token) {
        return Optional.ofNullable(extractClaim(token, claims -> {
            Object value = claims.get(CLAIM_ROLE);
            return value != null ? value.toString() : null;
        }));
    }

    /**
     * Проверка, является ли токен refresh-токеном.
     */
    public boolean isRefreshToken(String token) {
        String tokenType = extractClaim(token, claims -> {
            Object value = claims.get(CLAIM_TOKEN_TYPE);
            return value != null ? value.toString() : null;
        });
        return TOKEN_TYPE_REFRESH.equals(tokenType);
    }

    /**
     * Утилита для извлечения конкретного claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлечение всех claims.
     * Бросает JwtException при невалидном токене.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Извлечение даты истечения.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Проверка истечения токена.
     */
    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * Базовая валидация JWT токена:
     * - корректная подпись,
     * - не истёк,
     * - содержит обязательные claim'ы (subject и userId).
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                log.warn("JWT token has no subject");
                return false;
            }
            Object userId = claims.get(CLAIM_USER_ID);
            if (userId == null) {
                log.warn("JWT token has no userId claim");
                return false;
            }
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Здесь важно не логировать сам токен (чтобы не утечь секрет/данные)
            log.warn("Invalid JWT token: {} ({})", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Валидация токена для конкретного пользователя.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equals(userDetails.getUsername())
                && validateToken(token);
    }

    /**
     * Получение ключа подписи из секрета.
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
