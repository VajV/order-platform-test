package com.orderplatform.order.service;

import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.entity.OrderTimeout;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.repository.OrderTimeoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTimeoutHandler {

    private final OrderTimeoutRepository timeoutRepository;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 30000) // Проверка каждые 30 сек
    @Transactional
    public void handleExpiredOrders() {
        List<OrderTimeout> expiredTimeouts = timeoutRepository.findByExpiresAtBefore(
                LocalDateTime.now()
        );

        if (expiredTimeouts.isEmpty()) {
            return;
        }

        log.warn("Found {} expired order timeouts", expiredTimeouts.size());

        for (OrderTimeout timeout : expiredTimeouts) {
            handleTimeout(timeout);
        }
    }

    // ✅ ИСПРАВЛЕНО: Изменено на protected
    @Transactional
    protected void handleTimeout(OrderTimeout timeout) {
        Order order = orderRepository.findByIdForUpdate(timeout.getOrderId())
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for timeout: {}", timeout.getOrderId());
            timeoutRepository.delete(timeout);
            return;
        }

        log.warn("Order {} timeout waiting for: {}",
                timeout.getOrderId(), timeout.getExpectedEvent());

        // Отменяем заказ
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Process timeout while waiting for: " + timeout.getExpectedEvent());
        orderRepository.save(order);

        // Отправляем сигнал отмены
        if (previousStatus == OrderStatus.RESERVED) {
            publishInventoryReleaseEvent(order.getId());
        }

        publishOrderCancelledEvent(order.getId(), "Timeout");

        // Удаляем timeout
        timeoutRepository.delete(timeout);
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