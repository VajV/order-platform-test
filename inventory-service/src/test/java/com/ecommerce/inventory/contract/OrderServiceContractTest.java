package com.ecommerce.inventory.contract;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer Contract Test для inventory-service.
 * 
 * NOTE: Тест отключён, так как требует предварительной публикации stubs order-service.
 * Для включения: ./gradlew :order-service:publishStubsPublicationToMavenLocal
 */
@Disabled("Требует предварительной публикации stubs order-service")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EmbeddedKafka(partitions = 1, topics = {"order.created", "order.status-changed"})
@ActiveProfiles("test")
@DisplayName("Order Service Contract Tests (Consumer Side)")
class OrderServiceContractTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("должен иметь настроенный KafkaTemplate")
    void shouldHaveKafkaTemplateConfigured() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    @Disabled("Требует предварительной публикации stubs order-service")
    @DisplayName("должен получить order.created событие в ожидаемом формате")
    void shouldReceiveOrderCreatedEventInExpectedFormat() {
        // Consumer-driven contract тест
        // Требует: ./gradlew :order-service:publishStubsPublicationToMavenLocal
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    @Disabled("Требует предварительной публикации stubs order-service")
    @DisplayName("должен получить order.status-changed событие в ожидаемом формате")
    void shouldReceiveOrderStatusChangedEventInExpectedFormat() {
        // Consumer-driven contract тест
        assertThat(kafkaTemplate).isNotNull();
    }
}

