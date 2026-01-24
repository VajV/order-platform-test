package com.orderplatform.order.consumer;

import com.orderplatform.order.dto.event.*;
import com.orderplatform.order.service.OrderService;
import com.orderplatform.order.service.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OrderService orderService;

    // Слушаем события о успешном резервировании от inventory-service
    @KafkaListener(
            topics = "inventory.reserved",
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onInventoryReserved(
            @Payload InventoryReservedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received InventoryReservedEvent for order: {}", event.getOrderId());

        // Проверка на идемпотентность
        String eventId = key + ":inventory.reserved";
        if (orderService.isEventProcessed(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        try {
            sagaOrchestrator.onInventoryReserved(event);
            orderService.markEventAsProcessed(eventId);
        } catch (Exception e) {
            log.error("Error processing InventoryReservedEvent", e);
            throw e; // Requeue message
        }
    }

    // Слушаем события о НЕУДАЧНОМ резервировании
    @KafkaListener(
            topics = "inventory.failed",
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onInventoryFailed(
            @Payload InventoryFailedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.warn("Received InventoryFailedEvent for order: {}", event.getOrderId());

        String eventId = key + ":inventory.failed";
        if (orderService.isEventProcessed(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        try {
            sagaOrchestrator.onInventoryFailed(event);
            orderService.markEventAsProcessed(eventId);
        } catch (Exception e) {
            log.error("Error processing InventoryFailedEvent", e);
            throw e;
        }
    }

    // Слушаем события об успешном платеже
    @KafkaListener(
            topics = "payment.completed",
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received PaymentCompletedEvent for order: {}", event.getOrderId());

        String eventId = key + ":payment.completed";
        if (orderService.isEventProcessed(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        try {
            sagaOrchestrator.onPaymentCompleted(event);
            orderService.markEventAsProcessed(eventId);
        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent", e);
            throw e;
        }
    }

    // Слушаем события о НЕУДАЧНОМ платеже
    @KafkaListener(
            topics = "payment.failed",
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentFailed(
            @Payload PaymentFailedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.warn("Received PaymentFailedEvent for order: {}", event.getOrderId());

        String eventId = key + ":payment.failed";
        if (orderService.isEventProcessed(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        try {
            sagaOrchestrator.onPaymentFailed(event);
            orderService.markEventAsProcessed(eventId);
        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent", e);
            throw e;
        }
    }
}
