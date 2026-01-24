package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductEvent;
import com.ecommerce.product.kafka.ProductKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для публикации событий товаров в Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final ProductKafkaProducer kafkaProducer;

    /**
     * Публикует событие товара в Kafka topic
     *
     * @param event событие товара
     * @param topic Kafka topic для публикации
     */
    public void publishProductEvent(ProductEvent event, String topic) {
        try {
            log.info("Publishing product event: {} to topic: {}", event.getEventType(), topic);
            log.debug("Event details: productId={}, productName={}",
                    event.getProductId(), event.getProductName());

            kafkaProducer.sendProductEvent(topic, event);

            log.info("Product event published successfully: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish product event: {} to topic: {}",
                    event.getEventType(), topic, e);

            // В production использовать Dead Letter Queue (DLQ)
            // или retry механизм для критичных событий
            // Например: retryTemplate.execute(...)

            // Можно также сохранить в БД для последующей отправки
            // saveFailedEvent(event, topic, e.getMessage());
        }
    }

    /**
     * Публикует асинхронно (для будущей реализации)
     */
    public void publishProductEventAsync(ProductEvent event, String topic) {
        // TODO: Использовать @Async для асинхронной публикации
        // TODO: Использовать CompletableFuture для отслеживания результата
        log.info("Async publishing not implemented yet");
        publishProductEvent(event, topic);
    }

    /**
     * Сохраняет неудачное событие для повторной отправки (для будущей реализации)
     */
    private void saveFailedEvent(ProductEvent event, String topic, String errorMessage) {
        // TODO: Сохранить в таблицу failed_events
        // TODO: Создать scheduled job для повторной отправки
        log.error("Saving failed event to database for retry: {}", event.getEventType());
    }
}
