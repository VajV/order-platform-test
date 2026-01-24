package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.event.InventoryReservedEvent;
import com.ecommerce.inventory.event.ReservationCompensatedEvent;
import com.ecommerce.inventory.event.ReservationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INVENTORY_RESERVED_TOPIC = "inventory.reserved";
    private static final String INVENTORY_FAILED_TOPIC = "inventory.failed";
    private static final String INVENTORY_COMPENSATED_TOPIC = "inventory.compensated";

    /**
     * Отправляет событие успешной резервации товара
     */
    public void sendReservationSuccess(String orderId, String productId, Integer quantity, String reservationId) {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .reservationId(reservationId)
                .timestamp(LocalDateTime.now().toString())
                .build();

        Message<InventoryReservedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, INVENTORY_RESERVED_TOPIC)
                .setHeader(KafkaHeaders.KEY, orderId)  // ← ИСПРАВЛЕНО
                .build();

        kafkaTemplate.send(message);
        log.info("Sent inventory reserved event for order: {}, product: {}", orderId, productId);
    }

    /**
     * Отправляет событие неудачной резервации (недостаточно товара)
     */
    public void sendReservationFailed(String orderId, String productId, Integer requestedQuantity, String reason) {
        ReservationFailedEvent event = ReservationFailedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .requestedQuantity(requestedQuantity)
                .reason(reason)
                .timestamp(LocalDateTime.now().toString())
                .build();

        Message<ReservationFailedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, INVENTORY_FAILED_TOPIC)
                .setHeader(KafkaHeaders.KEY, orderId)  // ← ИСПРАВЛЕНО
                .build();

        kafkaTemplate.send(message);
        log.warn("Sent inventory reservation failed event for order: {}, reason: {}", orderId, reason);
    }

    /**
     * Отправляет событие компенсации резервации (сага откатывается)
     */
    public void sendReservationCompensated(String orderId, String productId, Integer quantity, String reservationId, String reason) {
        ReservationCompensatedEvent event = ReservationCompensatedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .reservationId(reservationId)
                .reason(reason)
                .timestamp(LocalDateTime.now().toString())
                .build();

        Message<ReservationCompensatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, INVENTORY_COMPENSATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, orderId)  // ← ИСПРАВЛЕНО
                .build();

        kafkaTemplate.send(message);
        log.info("Sent inventory reservation compensated event for order: {}", orderId);
    }
}
