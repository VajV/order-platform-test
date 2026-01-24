package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.dto.OrderCancelledEvent;
import com.ecommerce.inventory.dto.OrderCreatedEvent;
import com.ecommerce.inventory.dto.PaymentConfirmedEvent;
import com.ecommerce.inventory.dto.ReservationRequest;
import com.ecommerce.inventory.entity.ProcessedEvent;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import com.ecommerce.inventory.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Listens to order-service events and triggers inventory reservations.
 * Implements idempotency via ProcessedEvent tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ReservationService reservationService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Listen to order.created topic.
     * When new order arrives, attempt to reserve stock for each item.
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderCreated(String message) {
        log.info("Received order.created event: {}", message);

        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            // Idempotency check
            String eventId = event.getOrderId() + ":order.created";
            if (processedEventRepository.existsById(eventId)) {
                log.warn("Event already processed: {}", eventId);
                return;
            }

            // Reserve stock for each item in the order
            for (OrderCreatedEvent.OrderItemDto item : event.getItems()) {
                ReservationRequest request = ReservationRequest.builder()
                        .orderId(event.getOrderId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build();
                reservationService.reserveStock(request);
            }

            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
            log.info("Successfully reserved stock for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order.created event", e);
            throw new RuntimeException("Failed to process order.created event", e);
        }
    }

    /**
     * Listen to order.payment-confirmed topic.
     * When payment confirmed, move reservation from PENDING to CONFIRMED.
     */
    @KafkaListener(
            topics = "order.payment-confirmed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentConfirmed(String message) {
        log.info("Received order.payment-confirmed event: {}", message);

        try {
            PaymentConfirmedEvent event = objectMapper.readValue(message, PaymentConfirmedEvent.class);

            // Idempotency check
            String eventId = event.getOrderId() + ":payment.confirmed";
            if (processedEventRepository.existsById(eventId)) {
                log.warn("Event already processed: {}", eventId);
                return;
            }

            // Confirm the reservation
            reservationService.confirmReservation(event.getOrderId());

            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
            log.info("Successfully confirmed reservation for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing payment.confirmed event", e);
            throw new RuntimeException("Failed to process payment.confirmed event", e);
        }
    }

    /**
     * Listen to order.cancelled topic.
     * When order cancelled, release reserved stock.
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderCancelled(String message) {
        log.info("Received order.cancelled event: {}", message);

        try {
            OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);

            // Idempotency check
            String eventId = event.getOrderId() + ":order.cancelled";
            if (processedEventRepository.existsById(eventId)) {
                log.warn("Event already processed: {}", eventId);
                return;
            }

            // Release the reservation
            reservationService.releaseReservation(event.getOrderId());

            // Mark event as processed
            processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
            log.info("Successfully released reservation for cancelled order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order.cancelled event", e);
            throw new RuntimeException("Failed to process order.cancelled event", e);
        }
    }
}
