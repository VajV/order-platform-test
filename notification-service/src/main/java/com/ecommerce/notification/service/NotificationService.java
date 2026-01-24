package com.ecommerce.notification.service;

import com.ecommerce.notification.exception.RateLimitExceededException;
import com.ecommerce.notification.exception.TemplateNotFoundException;
import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.model.NotificationTemplate;
import com.ecommerce.notification.repository.NotificationLogRepository;
import com.ecommerce.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final EmailService emailService;
    private final RedisRateLimiter rateLimiter;

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    /**
     * Отправить уведомление по типу события
     */
    public void sendNotification(String eventType, String userId, String userEmail,
                                 Map<String, String> variables) {
        try {
            // 1. Rate limiting check
            if (!rateLimiter.allowRequest(userEmail)) {
                logNotification(userId, userEmail, eventType, null, null,
                        NotificationLog.NotificationStatus.RATE_LIMITED, "Rate limit exceeded");
                throw new RateLimitExceededException("Rate limit exceeded for email: " + userEmail);
            }

            // 2. Get template
            Optional<NotificationTemplate> templateOpt =
                    templateRepository.findByEventTypeAndEnabled(eventType, true);

            if (templateOpt.isEmpty()) {
                log.warn("⚠️ Template not found or disabled for event type: {}", eventType);
                throw new TemplateNotFoundException("Template not found for event: " + eventType);
            }

            NotificationTemplate template = templateOpt.get();

            // 3. Render template
            String subject = renderTemplate(template.getSubjectTemplate(), variables);
            String body = renderTemplate(template.getBodyTemplate(), variables);

            // 4. Send email
            emailService.send(userEmail, subject, body);

            // 5. Log success
            logNotification(userId, userEmail, eventType, subject, body,
                    NotificationLog.NotificationStatus.SENT, null);

            log.info("✅ Notification sent. Event: {}, UserId: {}, Email: {}",
                    eventType, userId, userEmail);

        } catch (RateLimitExceededException | TemplateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to send notification. Event: {}, UserId: {}, Email: {}",
                    eventType, userId, userEmail, e);
            logNotification(userId, userEmail, eventType, null, null,
                    NotificationLog.NotificationStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Рендер шаблона с заменой {{variable}}
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = variables.getOrDefault(varName, "N/A");
            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Сохранить лог отправки
     */
    private void logNotification(String userId, String email, String eventType,
                                 String subject, String body,
                                 NotificationLog.NotificationStatus status,
                                 String errorMessage) {
        NotificationLog log = NotificationLog.builder()
                .userId(userId)
                .email(email)
                .eventType(eventType)
                .subject(subject)
                .body(body)
                .status(status)
                .errorMessage(errorMessage)
                .sentAt(status == NotificationLog.NotificationStatus.SENT ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .build();

        logRepository.save(log);
    }

    /**
     * История уведомлений пользователя за последний час
     */
    public List<NotificationLog> getUserNotificationHistory(String userId) {
        return logRepository.findByUserIdAndCreatedAtAfter(
                userId,
                LocalDateTime.now().minusHours(1)
        );
    }

    /**
     * Последние 10 уведомлений
     */
    public List<NotificationLog> getRecentNotifications() {
        return logRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Логирование уведомления без отправки (для событий без email)
     */
    public void logNotificationWithoutSending(String eventType, String userId, Map<String, String> variables) {
        logNotification(userId, null, eventType, null, variables.toString(),
                NotificationLog.NotificationStatus.PENDING, "No email provided - logged only");
        log.info("📝 Logged notification without sending. Event: {}, UserId: {}", eventType, userId);
    }
}
