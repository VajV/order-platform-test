package com.orderplatform.order.contract;

import com.orderplatform.order.domain.enums.OrderStatus;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Базовый класс для Contract Tests для Kafka messaging.
 * 
 * Тестирует producer-сторону: order-service публикует события,
 * и мы проверяем что они соответствуют контрактам.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(MockitoExtension.class)
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
public abstract class MessagingBaseContractTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Триггер для контракта orderCreatedEvent.
     * Вызывается Spring Cloud Contract при проверке контракта.
     */
    public void publishOrderCreatedEvent() {
        Map<String, Object> event = Map.of(
            "orderId", 1L,
            "userId", 100L,
            "items", List.of(Map.of(
                "productId", "PROD-001",
                "productName", "Test Product",
                "quantity", 2,
                "unitPrice", 99.99
            )),
            "totalPrice", new BigDecimal("199.98"),
            "timestamp", LocalDateTime.now().toString()
        );

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "order.created")
                .setHeader(KafkaHeaders.KEY, "1")
                .setHeader("contentType", "application/json")
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * Триггер для контракта orderStatusChangedEvent.
     */
    public void publishOrderStatusChangedEvent() {
        Map<String, Object> event = Map.of(
            "orderId", 1L,
            "previousStatus", OrderStatus.NEW.name(),
            "newStatus", OrderStatus.RESERVED.name(),
            "reason", "Inventory reserved successfully",
            "timestamp", LocalDateTime.now().toString()
        );

        Message<Map<String, Object>> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "order.status-changed")
                .setHeader(KafkaHeaders.KEY, "1")
                .setHeader("contentType", "application/json")
                .build();

        kafkaTemplate.send(message);
    }
}

