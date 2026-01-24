package com.ecommerce.service;

import com.ecommerce.notification.service.DevEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DevEmailServiceTest {

    @InjectMocks
    private DevEmailService emailService;

    @Test
    void testSendEmail_LogsToConsole() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String body = "<html><body>Test Body</body></html>";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> emailService.send(to, subject, body));
    }

    @Test
    void testSendEmail_WithNullValues() {
        // Should not throw exception even with nulls
        assertDoesNotThrow(() -> emailService.send(null, null, null));
    }
}
