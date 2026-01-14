package com.ecommerce.service;

import com.ecommerce.notification.exception.RateLimitExceededException;
import com.ecommerce.notification.exception.TemplateNotFoundException;
import com.ecommerce.notification.model.NotificationLog;
import com.ecommerce.notification.model.NotificationTemplate;
import com.ecommerce.notification.repository.NotificationLogRepository;
import com.ecommerce.notification.repository.NotificationTemplateRepository;
import com.ecommerce.notification.service.EmailService;
import com.ecommerce.notification.service.NotificationService;
import com.ecommerce.notification.service.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RedisRateLimiter rateLimiter;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationTemplate template;
    private Map<String, String> variables;

    @BeforeEach
    void setUp() {
        template = NotificationTemplate.builder()
                .id("1")
                .eventType("order.created")
                .subjectTemplate("Order {{orderId}} created")
                .bodyTemplate("Hello {{userId}}, your order {{orderId}} is created!")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        variables = new HashMap<>();
        variables.put("orderId", "ORDER-123");
        variables.put("userId", "user-456");
    }

    @Test
    void testSendNotification_Success() throws Exception {
        // Given
        when(rateLimiter.allowRequest(anyString())).thenReturn(true);
        when(templateRepository.findByEventTypeAndEnabled("order.created", true))
                .thenReturn(Optional.of(template));
        doNothing().when(emailService).send(anyString(), anyString(), anyString());
        when(logRepository.save(any(NotificationLog.class))).thenReturn(null);

        // When
        notificationService.sendNotification("order.created", "user-456", "test@example.com", variables);

        // Then
        verify(rateLimiter).allowRequest("test@example.com");
        verify(templateRepository).findByEventTypeAndEnabled("order.created", true);
        verify(emailService).send(eq("test@example.com"), contains("ORDER-123"), contains("ORDER-123"));
        verify(logRepository).save(any(NotificationLog.class));
    }

    @Test
    void testSendNotification_RateLimitExceeded() {
        // Given
        when(rateLimiter.allowRequest(anyString())).thenReturn(false);

        // When & Then
        assertThrows(RateLimitExceededException.class, () ->
                notificationService.sendNotification("order.created", "user-456", "test@example.com", variables)
        );

        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void testSendNotification_TemplateNotFound() {
        // Given
        when(rateLimiter.allowRequest(anyString())).thenReturn(true);
        when(templateRepository.findByEventTypeAndEnabled("order.created", true))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TemplateNotFoundException.class, () ->
                notificationService.sendNotification("order.created", "user-456", "test@example.com", variables)
        );

        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }
}
