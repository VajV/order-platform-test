package com.orderplatform.order.service;

import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.entity.OrderTimeout;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.event.*;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.repository.OrderTimeoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final OrderTimeoutRepository timeoutRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Processing inventory reserved for order: {}", event.getOrderId());

        Order order = orderRepository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.NEW) {
            log.warn("Order {} is not in NEW status, ignoring event", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.RESERVED);
        orderRepository.save(order);

        log.info("Order {} transitioned to RESERVED", event.getOrderId());

        timeoutRepository.deleteById(event.getOrderId());
        createTimeout(event.getOrderId(), "payment.completed", 5);

        publishPaymentRequestedEvent(event.getOrderId());
    }

    @Transactional
    public void onInventoryFailed(InventoryFailedEvent event) {
        log.warn("Inventory reservation failed for order: {}", event.getOrderId());

        Order order = orderRepository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Inventory reservation failed: " + event.getReason());
        orderRepository.save(order);

        timeoutRepository.deleteById(event.getOrderId());
        publishOrderCancelledEvent(event.getOrderId(), event.getReason());
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for order: {}", event.getOrderId());

        Order order = orderRepository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.RESERVED) {
            log.warn("Order {} is not in RESERVED status, ignoring event", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(event.getPaymentId());
        orderRepository.save(order);

        log.info("Order {} transitioned to PAID", event.getOrderId());

        timeoutRepository.deleteById(event.getOrderId());
        publishShippingRequestedEvent(event.getOrderId());
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed for order: {}", event.getOrderId());

        Order order = orderRepository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.RESERVED) {
            log.warn("Order {} is not in RESERVED status, cannot handle payment failure", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Payment failed: " + event.getReason());
        orderRepository.save(order);

        timeoutRepository.deleteById(event.getOrderId());

        publishInventoryReleaseEvent(event.getOrderId());
        publishOrderCancelledEvent(event.getOrderId(), event.getReason());
    }

    // ✅ ИСПРАВЛЕНО: Изменено на protected и использован Duration
    @Transactional
    protected void createTimeout(Long orderId, String expectedEvent, int minutes) {
        OrderTimeout timeout = OrderTimeout.builder()
                .orderId(orderId)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMinutes(minutes)))
                .expectedEvent(expectedEvent)
                .build();

        timeoutRepository.save(timeout);
    }

    private void publishPaymentRequestedEvent(Long orderId) {
        Message<Long> message = MessageBuilder
                .withPayload(orderId)
                .setHeader(KafkaHeaders.TOPIC, "order.payment-requested")
                .setHeader("kafka_messageKey", orderId.toString())
                .build();

        kafkaTemplate.send(message);
    }

    private void publishShippingRequestedEvent(Long orderId) {
        Message<Long> message = MessageBuilder
                .withPayload(orderId)
                .setHeader(KafkaHeaders.TOPIC, "order.ready-to-ship")
                .setHeader("kafka_messageKey", orderId.toString())
                .build();

        kafkaTemplate.send(message);
    }

    private void publishInventoryReleaseEvent(Long orderId) {
        Message<Long> message = MessageBuilder
                .withPayload(orderId)
                .setHeader(KafkaHeaders.TOPIC, "inventory.release-reservation")
                .setHeader("kafka_messageKey", orderId.toString())
                .build();

        kafkaTemplate.send(message);
    }

    private void publishOrderCancelledEvent(Long orderId, String reason) {
        Message<String> message = MessageBuilder
                .withPayload(reason)
                .setHeader(KafkaHeaders.TOPIC, "order.cancelled")
                .setHeader("kafka_messageKey", orderId.toString())
                .build();

        kafkaTemplate.send(message);
    }
}