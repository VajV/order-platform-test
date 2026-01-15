package com.ecommerce.auth.config;

import com.ecommerce.auth.security.JwtAuthenticationFilter;
import com.ecommerce.auth.security.JwtExceptionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration
 *
 * ✅ FEATURES:
 * - JWT-based stateless authentication
 * - CORS configured for dev frontends
 * - BCrypt password encoding (strength 12)
 * - Custom JWT filters
 * - DaoAuthenticationProvider explicitly registered
 * - CSRF disabled (JWT doesn't need it)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityBeans {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ============================================================
                // CORS Configuration
                // ============================================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ============================================================
                // CSRF Disabled (stateless JWT auth)
                // ============================================================
                .csrf(csrf -> csrf.disable())

                // ============================================================
                // Authentication Provider (ВАЖНО!)
                // ============================================================
                .authenticationProvider(authenticationProvider())

                // ============================================================
                // Authorization Configuration
                // ============================================================
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no token required)
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // ============================================================
                // Session Management (STATELESS for JWT)
                // ============================================================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ============================================================
                // JWT Filters (ORDER IS IMPORTANT!)
                // Exception filter FIRST, then Authentication filter
                // ============================================================
                .addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration Source
     *
     * Разрешает запросы с локальных dev серверов (localhost:3000, 4200, 5173).
     * Для production измени на свои домены!
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (localhost for dev)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",      // React dev
                "http://localhost:4200",      // Angular dev
                "http://localhost:5173",      // Vite dev
                "http://127.0.0.1:3000",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:5173"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers (all)
        configuration.setAllowedHeaders(List.of("*"));

        // Headers exposed to client
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Count",
                "X-Page-Number"
        ));

        // Allow credentials (cookies/tokens)
        configuration.setAllowCredentials(true);

        // Max age for preflight (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Authentication Manager Bean
     *
     * Требуется для AuthService.login() где используется
     * authenticationManager.authenticate(token)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password Encoder Bean
     *
     * BCrypt с strength 12 (default 10).
     * Больше strength = медленнее = безопаснее.
     * Для production рекомендуется 12+.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DAO Authentication Provider Bean
     *
     * Использует UserDetailsService и PasswordEncoder для аутентификации.
     * ✅ ВАЖНО: Явно зарегистрирован в securityFilterChain()
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Сравнивает пароли: passwordEncoder.matches(rawPassword, encodedPassword)
        return provider;
    }
}
