package com.ecommerce.integration;

import com.ecommerce.notification.model.NotificationTemplate;
import com.ecommerce.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MongoDBIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer(
            DockerImageName.parse("mongo:7")
    );

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

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
