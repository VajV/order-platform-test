package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(
            JwtValidationGatewayFilterFactory.class
    );

    // ✅ ТОЛЬКО ОДНО ПОЛЕ! Удаляем дублирование
    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtValidationGatewayFilterFactory() {
        super(Config.class);
    }

    // ✅ ПРОВЕРЯЕМ КОНФИГ ПРИ СТАРТЕ (не при каждом запросе!)
    @PostConstruct
    public void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "❌ КРИТИЧЕСКАЯ ОШИБКА: jwt.secret не установлен в переменных окружения!\n" +
                            "   Установи: export JWT_SECRET='твой-секретный-ключ-32+символа'"
            );
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "❌ КРИТИЧЕСКАЯ ОШИБКА: jwt.secret должен быть минимум 32 символа!\n" +
                            "   Текущая длина: " + jwtSecret.length() + "\n" +
                            "   Примеры хороших ключей:\n" +
                            "   - ThisIsA32CharacterSecretKey123\n" +
                            "   - J9#mK2$vL8*pQ4@xY1%zB5&nW3^tR6!"
            );
        }

        logger.info("✅ JWT конфигурация валидна (длина: {})", jwtSecret.length());
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1️⃣ ИЗВЛЕКАЕМ ТОКЕН ИЗ ЗАГОЛОВКА
            String authHeader = exchange.getRequest().getHeaders()
                    .getFirst("Authorization");

            String requestPath = exchange.getRequest().getURI().getPath();
            String requestMethod = exchange.getRequest().getMethod().toString();

            logger.debug("🔍 Проверяю токен: {} {}", requestMethod, requestPath);

            // 2️⃣ ПРОВЕРЯЕМ НАЛИЧИЕ ЗАГОЛОВКА
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("❌ Отсутствует JWT токен для: {} {}",
                        requestMethod, requestPath);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders()
                        .add("WWW-Authenticate", "Bearer realm=\"gateway\"");

                return exchange.getResponse().setComplete();
            }

            try {
                // 3️⃣ ИЗВЛЕКАЕМ ТОКЕН (убираем "Bearer " префикс)
                String token = authHeader.substring(7);

                // 4️⃣ СОЗДАЁМ КЛЮЧ ДЛЯ ПРОВЕРКИ
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

                // 5️⃣ ПАРСИМ И ВАЛИДИРУЕМ ТОКЕН
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 6️⃣ ИЗВЛЕКАЕМ ДАННЫЕ ИЗ ТОКЕНА
                String userId = claims.get("userId", String.class);
                String roles = claims.get("roles", String.class);
                String username = claims.getSubject();

                logger.info("✅ JWT валидирован для пользователя: {} (ID: {})",
                        username, userId);

                // 7️⃣ ПЕРЕДАЁМ ДАННЫЕ ДАЛЬШЕ (добавляем в заголовки)
                exchange.getRequest().mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Roles", roles != null ? roles : "ROLE_USER")
                        .header("X-User-Name", username != null ? username : "")
                        .build();

                // 8️⃣ ОТПУСКАЕМ ЗАПРОС ДАЛЬШЕ К МИКРОСЕРВИСУ
                return chain.filter(exchange);

            } catch (JwtException e) {
                // ОШИБКА ВАЛИДАЦИИ ТОКЕНА
                logger.error("❌ Невалидный JWT токен: {}", e.getMessage());

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders()
                        .add("WWW-Authenticate",
                                "Bearer error=\"invalid_token\", error_description=\""
                                        + e.getMessage() + "\"");

                return exchange.getResponse().setComplete();

            } catch (IllegalArgumentException e) {
                // ПРОБЛЕМА С САМИМ КЛЮЧОМ
                logger.error("❌ Ошибка конфигурации JWT: {}", e.getMessage());

                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

                return exchange.getResponse().setComplete();
            }
        };
    }

    // Конфигурационный класс (пока пустой, но может расширяться)
    public static class Config {
        private boolean enableLogging = true;

        public boolean isEnableLogging() {
            return enableLogging;
        }

        public void setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
        }
    }
}
