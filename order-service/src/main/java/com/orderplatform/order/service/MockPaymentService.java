package com.orderplatform.order.service;

import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.dto.event.PaymentCompletedEvent;
import com.orderplatform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock Payment Service для автоматического тестирования полного lifecycle заказа.
 * Слушает order.payment-requested и автоматически отправляет payment.completed.
 * 
 * Активируется только в профилях: dev, docker, test
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile({"dev", "docker", "test"})
public class MockPaymentService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${mock.payment.delay-ms:1000}")
    private long paymentDelayMs;

    @Value("${mock.payment.success-rate:1.0}")
    private double successRate;

    @KafkaListener(
            topics = "order.payment-requested",
            groupId = "mock-payment-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentRequest(String message, Acknowledgment ack) {
        log.info("[MockPayment] Received payment request: {}", message);

        try {
            Long orderId = Long.parseLong(message.trim());
            
            // Имитируем задержку обработки платежа
            scheduler.schedule(() -> processPayment(orderId), paymentDelayMs, TimeUnit.MILLISECONDS);
            
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[MockPayment] Error processing payment request", e);
            ack.acknowledge();
        }
    }

    private void processPayment(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("[MockPayment] Order not found: {}", orderId);
                return;
            }

            // Проверяем success rate для имитации неудачных платежей
            boolean success = Math.random() < successRate;

            if (success) {
                String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .orderId(orderId)
                        .paymentId(paymentId)
                        .amount(order.getTotalPrice())
                        .build();

                kafkaTemplate.send("payment.completed", orderId.toString(), event);
                log.info("[MockPayment] Payment completed for order {}, paymentId: {}", orderId, paymentId);
            } else {
                log.info("[MockPayment] Payment failed for order {} (simulated failure)", orderId);
                kafkaTemplate.send("payment.failed", orderId.toString(), 
                        java.util.Map.of("orderId", orderId, "reason", "Simulated payment failure"));
            }
        } catch (Exception e) {
            log.error("[MockPayment] Error processing payment for order {}", orderId, e);
        }
    }
}
