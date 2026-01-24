package com.ecommerce.product.kafka;

import com.ecommerce.product.dto.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer для отправки событий товаров
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaProducer {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    /**
     * Отправляет событие товара в Kafka topic
     *
     * @param topic название Kafka topic
     * @param event событие товара
     */
    public void sendProductEvent(String topic, ProductEvent event) {
        try {
            log.info("Sending product event to Kafka topic '{}': {}", topic, event.getEventType());
            log.debug("Event details - Product ID: {}, Name: {}",
                    event.getProductId(), event.getProductName());

            // Создаем сообщение с заголовками
            Message<ProductEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, event.getProductId().toString())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("source", event.getSource())
                    .setHeader("timestamp", event.getTimestamp().toString())
                    .build();

            // Отправляем асинхронно
            CompletableFuture<SendResult<String, ProductEvent>> future =
                    kafkaTemplate.send(message);

            // Обрабатываем результат отправки
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Product event sent successfully to topic '{}': {} (partition: {}, offset: {})",
                            topic,
                            event.getEventType(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                } else {
                    log.error("Failed to send product event to topic '{}': {}",
                            topic, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error sending product event to Kafka topic '{}': {}",
                    topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send product event to Kafka", e);
        }
    }

    /**
     * Отправляет событие синхронно (ждет подтверждения)
     */
    public void sendProductEventSync(String topic, ProductEvent event) {
        try {
            log.info("Sending product event synchronously to topic '{}': {}",
                    topic, event.getEventType());

            Message<ProductEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, event.getProductId().toString())
                    .build();

            SendResult<String, ProductEvent> result = kafkaTemplate.send(message).get();

            log.info("Product event sent synchronously: {} (partition: {}, offset: {})",
                    event.getEventType(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

        } catch (Exception e) {
            log.error("Error sending product event synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send product event synchronously", e);
        }
    }
}
