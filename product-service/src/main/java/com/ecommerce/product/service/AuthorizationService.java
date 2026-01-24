package com.ecommerce.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Сервис для проверки авторизации и прав доступа
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    /**
     * Проверяет, является ли текущий пользователь администратором
     */
    public boolean isAdmin() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            log.debug("No authentication found");
            return false;
        }

        boolean hasAdminRole = hasRole(auth, "ROLE_ADMIN");
        log.debug("User has admin role: {}", hasAdminRole);
        return hasAdminRole;
    }

    /**
     * Проверяет, является ли вызов от внутреннего микросервиса
     */
    public boolean isInternalService() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            log.debug("No authentication found for internal service check");
            return false;
        }

        boolean isInternal = hasRole(auth, "ROLE_INTERNAL_SERVICE");
        log.debug("Request is from internal service: {}", isInternal);
        return isInternal;
    }

    /**
     * Проверяет, является ли текущий пользователь обычным пользователем
     */
    public boolean isUser() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            log.debug("No authentication found");
            return false;
        }

        boolean hasUserRole = hasRole(auth, "ROLE_USER");
        log.debug("User has user role: {}", hasUserRole);
        return hasUserRole;
    }

    /**
     * Получает ID текущего пользователя из JWT claims
     */
    public Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String) {
            // В реальной реализации извлекать из JWT claims
            // Пример: return jwtUtil.getUserIdFromJwt(token);
            log.debug("Getting current user ID from authentication");
            // TODO: Реализовать извлечение userId из JWT claims
            return null;
        }
        log.debug("No user ID found in authentication");
        return null;
    }

    /**
     * Получает username текущего пользователя
     */
    public String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            log.debug("Current username: {}", username);
            return username;
        }
        log.debug("No username found in authentication");
        return null;
    }

    /**
     * Проверяет, имеет ли пользователь конкретную роль
     */
    public boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return false;
        }
        return hasRole(auth, role);
    }

    /**
     * Проверяет, имеет ли пользователь хотя бы одну из указанных ролей
     */
    public boolean hasAnyRole(String... roles) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return false;
        }

        for (String role : roles) {
            if (hasRole(auth, role)) {
                log.debug("User has role: {}", role);
                return true;
            }
        }
        log.debug("User doesn't have any of the required roles");
        return false;
    }

    /**
     * Проверяет, аутентифицирован ли пользователь
     */
    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated();
        log.debug("User is authenticated: {}", authenticated);
        return authenticated;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Получает текущую аутентификацию из SecurityContext
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Проверяет наличие роли у аутентификации
     */
    private boolean hasRole(Authentication auth, String role) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
