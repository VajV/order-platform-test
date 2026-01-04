package com.ecommerce.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * Извлекает и валидирует JWT токен из Authorization заголовка
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Извлечение Authorization заголовка
        final String authHeader = request.getHeader("Authorization");

        // Проверка наличия Bearer токена
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Извлечение JWT токена (удаляем "Bearer " префикс)
            final String jwt = authHeader.substring(7);

            // Извлечение username из токена
            final String username = jwtUtil.extractUsername(jwt);

            // Если username есть и аутентификация еще не установлена
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Загрузка пользователя из БД
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ✅ ИСПРАВЛЕНО: используем validateToken(token, userDetails)
                if (jwtUtil.validateToken(jwt, userDetails)) {

                    // Создание Authentication объекта
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Установка деталей запроса
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Установка аутентификации в SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("User '{}' authenticated successfully", username);
                } else {
                    log.warn("Invalid JWT token for user '{}'", username);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
