package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;  // ← ДОБАВИЛИ!
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    /**
     * ✅ ОСНОВНОЙ KeyResolver (по IP)
     *
     * @Primary = "Используй ЭТО по умолчанию"
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - Все маршруты, которые не указали конкретный resolver
     * - Защита от DDoS по IP адресу
     */
    @Bean
    @Primary  // ← ВОТ КЛЮЧЕВОЕ ИЗМЕНЕНИЕ!
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress()
                    .getAddress()
                    .getHostAddress()
                    : "unknown";

            logger.debug("🔑 Rate-limit key (IP): {}", ip);
            return Mono.just(ip);
        };
    }

    /**
     * ✅ АЛЬТЕРНАТИВНЫЙ KeyResolver (по User ID)
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - Только когда явно указан в application.yml
     * - Например: key-resolver: "#{@userIdKeyResolver}"
     *
     * ЗАЧЕМ НУЖЕН:
     * - Для ограничения КОНКРЕТНОГО пользователя (не IP)
     * - Полезно для заказов (1 пользователь = макс 50 заказов/минуту)
     */
    @Bean
    public KeyResolver userIdKeyResolver() {
        return exchange -> {
            // Пытаемся получить User ID из заголовка
            String userId = exchange.getRequest().getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                logger.debug("🔑 Rate-limit key (User): {}", userId);
                return Mono.just(userId);
            }

            // Fallback на IP если нет User ID
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress()
                    .getAddress()
                    .getHostAddress()
                    : "unknown";

            logger.debug("🔑 Rate-limit key (fallback to IP): {}", ip);
            return Mono.just(ip);
        };
    }
}
