package com.ecommerce.notification.controller;

import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.service.NotificationService;
import com.ecommerce.notification.service.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
     * Отправить тестовое уведомление (для debugging)
     */
    @PostMapping("/send-test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestParam String eventType,
            @RequestParam String userId,
            @RequestParam String userEmail) {

        log.info("🧪 Test notification request: eventType={}, userId={}, email={}",
                eventType, userId, userEmail);

        try {
            // Тестовые переменные
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
     * Проверить rate limit для email
     */
    @GetMapping("/rate-limit/{email}")
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
     * Сбросить rate limit (только для dev/test)
     */
    @PostMapping("/rate-limit/{email}/reset")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String email) {
        rateLimiter.resetLimit(email);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rate limit reset for: " + email
        ));
    }

    /**
     * История уведомлений пользователя
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<NotificationLog>> getUserHistory(@PathVariable String userId) {
        List<NotificationLog> history = notificationService.getUserNotificationHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Последние уведомления
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationLog>> getRecentNotifications() {
        List<NotificationLog> recent = notificationService.getRecentNotifications();
        return ResponseEntity.ok(recent);
    }
}
