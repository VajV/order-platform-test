package com.ecommerce.integration;

import com.ecommerce.notification.model.NotificationTemplate;
import com.ecommerce.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для MongoDB в Notification Service.
 * Использует Flapdoodle embedded MongoDB (Docker не требуется).
 */
@SpringBootTest(classes = com.ecommerce.notification.NotificationServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"order.created"})
@ActiveProfiles("test")
class MongoDBIntegrationTest {

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Test
    void testSaveAndFindTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType("test.event")
                .subjectTemplate("Test Subject {{var}}")
                .bodyTemplate("Test Body {{var}}")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        NotificationTemplate saved = templateRepository.save(template);

        // Then
        assertNotNull(saved.getId());

        Optional<NotificationTemplate> found = templateRepository.findByEventType("test.event");
        assertTrue(found.isPresent());
        assertEquals("Test Subject {{var}}", found.get().getSubjectTemplate());
    }

    @Test
    void testFindByEventTypeAndEnabled() {
        // Given
        NotificationTemplate template = NotificationTemplate.builder()
                .eventType("enabled.test")
                .subjectTemplate("Subject")
                .bodyTemplate("Body")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        templateRepository.save(template);

        // When
        Optional<NotificationTemplate> found =
                templateRepository.findByEventTypeAndEnabled("enabled.test", true);

        // Then
        assertTrue(found.isPresent());
        assertTrue(found.get().isEnabled());
    }
}
