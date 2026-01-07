// OrderEventListener.java
package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to order-service events and triggers inventory reservations.
 * TODO: In Phase 2, integrate with actual order.created events from Schema Registry.
 * MVP version uses simple JSON message structure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ReservationService reservationService;

    /**
     * Listen to order.created topic.
     * When new order arrives, attempt to reserve stock.
     * MVP: Simple event, Phase 2: Avro schema + full event model.
     *
     * @param message order event message
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(String message) {
        log.info("Received order.created event: {}", message);
        // MVP implementation - Phase 2 will add proper event parsing
        // TODO: Parse message to OrderCreatedEvent
        // TODO: Extract orderId, productId, quantity
        // TODO: Call reservationService.reserveStock()
    }

    /**
     * Listen to order.payment-confirmed topic.
     * When payment confirmed, move reservation from PENDING to CONFIRMED.
     *
     * @param message order event message
     */
    @KafkaListener(
            topics = "order.payment-confirmed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentConfirmed(String message) {
        log.info("Received order.payment-confirmed event: {}", message);
        // TODO: Parse orderId from message
        // TODO: Call reservationService.confirmReservation()
    }

    /**
     * Listen to order.cancelled topic.
     * When order cancelled, release reserved stock.
     *
     * @param message order event message
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(String message) {
        log.info("Received order.cancelled event: {}", message);
        // TODO: Parse orderId from message
        // TODO: Call reservationService.releaseReservation()
    }
}
