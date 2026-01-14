package com.ecommerce.notification.kafka;

import com.ecommerce.notification.model.*;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "notification-service")
    public void handleOrderCreated(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        log.info("📨 Received message from topic '{}', offset: {}", topic, offset);
        log.debug("Message content: {}", message);

        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            Map<String, String> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("userId", event.getUserId());
            variables.put("totalAmount", String.format("%.2f", event.getTotalAmount()));

            notificationService.sendNotification(
                    "order.created",
                    event.getUserId(),
                    event.getUserEmail(),
                    variables
            );

            log.info("✅ Successfully processed order.created event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing order.created event", e);
        }
    }

    @KafkaListener(topics = "order.status-changed", groupId = "notification-service")
    public void handleOrderStatusChanged(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        log.info("📨 Received message from topic '{}', offset: {}", topic, offset);

        try {
            OrderStatusChangedEvent event = objectMapper.readValue(message, OrderStatusChangedEvent.class);

            Map<String, String> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("status", translateStatus(event.getStatus()));
            variables.put("userId", event.getUserId());

            notificationService.sendNotification(
                    "order.status-changed",
                    event.getUserId(),
                    event.getUserEmail(),
                    variables
            );

            log.info("✅ Successfully processed order.status-changed event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing order.status-changed event", e);
        }
    }

    @KafkaListener(topics = "inventory.reserved", groupId = "notification-service")
    public void handleInventoryReserved(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        log.info("📨 Received message from topic '{}', offset: {}", topic, offset);

        try {
            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);

            Map<String, String> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("itemCount", String.valueOf(event.getItemCount()));
            variables.put("userId", event.getUserId());

            notificationService.sendNotification(
                    "inventory.reserved",
                    event.getUserId(),
                    event.getUserEmail(),
                    variables
            );

            log.info("✅ Successfully processed inventory.reserved event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing inventory.reserved event", e);
        }
    }

    @KafkaListener(topics = "payment.completed", groupId = "notification-service")
    public void handlePaymentCompleted(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        log.info("📨 Received message from topic '{}', offset: {}", topic, offset);

        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);

            Map<String, String> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("amount", String.format("%.2f", event.getAmount()));
            variables.put("userId", event.getUserId());

            notificationService.sendNotification(
                    "payment.completed",
                    event.getUserId(),
                    event.getUserEmail(),
                    variables
            );

            log.info("✅ Successfully processed payment.completed event for orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing payment.completed event", e);
        }
    }

    /**
     * Перевод статусов на русский
     */
    private String translateStatus(String status) {
        return switch (status.toUpperCase()) {
            case "NEW" -> "Новый";
            case "RESERVED" -> "Зарезервирован";
            case "PAID" -> "Оплачен";
            case "SHIPPED" -> "Отправлен";
            case "COMPLETED" -> "Завершён";
            case "CANCELLED" -> "Отменён";
            default -> status;
        };
    }
}
