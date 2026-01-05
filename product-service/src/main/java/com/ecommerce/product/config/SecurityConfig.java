package com.ecommerce.product.config;

import com.ecommerce.product.security.JwtAuthenticationFilter;
import com.ecommerce.product.security.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация Spring Security
 * Настраивает JWT аутентификацию и авторизацию
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF (используем JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Настраиваем CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless сессии (JWT не требует сессий)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Настраиваем авторизацию
                .authorizeHttpRequests(authz -> authz
                        // Публичные endpoints (без авторизации)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Публичные API endpoints
                        .requestMatchers(
                                "/api/products",
                                "/api/products/{id}",
                                "/api/products/search",
                                "/api/products/category/**",
                                "/api/products/stock/available",
                                "/api/products/rating/top"
                        ).permitAll()

                        // Все остальные требуют аутентификации
                        .anyRequest().authenticated()
                )

                // Добавляем JWT фильтры
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Настройка CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Разрешенные origins (в production указать конкретные домены)
        configuration.setAllowedOrigins(Arrays.asList("*"));

        // Разрешенные HTTP методы
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Разрешенные заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Разрешить отправку cookies (для JWT в cookies)
        configuration.setAllowCredentials(false);

        // Время кэширования preflight запросов
        configuration.setMaxAge(3600L);

        // Заголовки, которые клиент может читать
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
