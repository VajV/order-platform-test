package com.ecommerce.notification.controller;

import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.security.GatewayUserPrincipal;
import com.ecommerce.notification.service.NotificationService;
import com.ecommerce.notification.service.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final RedisRateLimiter rateLimiter;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notification-service",
                "version", "1.0.0"
        ));
    }

    /**
     * Отправить тестовое уведомление (для debugging) - только ADMIN
     */
    @PostMapping("/send-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestParam String eventType,
            @RequestParam String userId,
            @RequestParam String userEmail) {

        log.info("🧪 Test notification request: eventType={}, userId={}, email={}",
                eventType, userId, userEmail);

        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("orderId", "TEST-" + System.currentTimeMillis());
            variables.put("totalAmount", "9999.99");
            variables.put("status", "PROCESSING");
            variables.put("itemCount", "5");
            variables.put("amount", "9999.99");

            notificationService.sendNotification(eventType, userId, userEmail, variables);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test notification sent",
                    "eventType", eventType,
                    "email", userEmail
            ));

        } catch (Exception e) {
            log.error("❌ Failed to send test notification", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Проверить rate limit для email - только ADMIN
     */
    @GetMapping("/rate-limit/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@PathVariable String email) {
        long remaining = rateLimiter.getRemainingRequests(email);

        return ResponseEntity.ok(Map.of(
                "email", email,
                "remaining", remaining,
                "max_per_hour", 10,
                "can_send", remaining > 0
        ));
    }

    /**
     * Сбросить rate limit (только для ADMIN)
     */
    @PostMapping("/rate-limit/{email}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String email) {
        rateLimiter.resetLimit(email);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rate limit reset for: " + email
        ));
    }

    /**
     * История уведомлений пользователя - пользователь видит только свои уведомления
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getUserHistory(@PathVariable String userId) {
        String currentUserId = getCurrentUserId();

        // Проверка: пользователь может видеть только свои уведомления (или ADMIN видит все)
        if (!userId.equals(currentUserId) && !isAdmin()) {
            log.warn("🚫 Access denied: user {} tried to access notifications of user {}", currentUserId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied", "message", "You can only view your own notifications"));
        }

        List<NotificationLog> history = notificationService.getUserNotificationHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Мои уведомления - текущий пользователь
     */
    @GetMapping("/my")
    public ResponseEntity<List<NotificationLog>> getMyNotifications() {
        String currentUserId = getCurrentUserId();
        List<NotificationLog> history = notificationService.getUserNotificationHistory(currentUserId);
        return ResponseEntity.ok(history);
    }

    /**
     * Последние уведомления - только ADMIN
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationLog>> getRecentNotifications() {
        List<NotificationLog> recent = notificationService.getRecentNotifications();
        return ResponseEntity.ok(recent);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof GatewayUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}
