package com.orderplatform.order.service;

import org.springframework.kafka.support.KafkaHeaders;
import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.entity.OrderItem;
import com.orderplatform.order.domain.entity.OrderTimeout;
import com.orderplatform.order.domain.entity.ProcessedEvent;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.event.OrderCreatedEvent;
import com.orderplatform.order.dto.event.OrderStatusChangedEvent;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.exception.OrderNotFoundException;
import com.orderplatform.order.exception.InvalidStatusTransitionException;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.repository.OrderTimeoutRepository;
import com.orderplatform.order.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTimeoutRepository timeoutRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final long TIMEOUT_MINUTES = 5;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Создаем заказ
        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.NEW)
                .totalPrice(calculateTotal(request.getItems()))
                .build();

        // Добавляем товары
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .totalPrice(itemRequest.getUnitPrice().multiply(
                            new BigDecimal(itemRequest.getQuantity())
                    ))
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Публикуем событие order.created
        publishOrderCreatedEvent(savedOrder, request.getItems());

        // Создаем timeout для ожидания резервирования
        createTimeout(savedOrder.getId(), "inventory.reserved");

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, String reason) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (!order.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + order.getStatus() + " to " + newStatus
            );
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, previousStatus, newStatus);

        // Публикуем событие об изменении статуса
        publishOrderStatusChangedEvent(updatedOrder, previousStatus, reason);

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        log.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED ||
                order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel order in status: " + order.getStatus()
            );
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        Order updatedOrder = orderRepository.save(order);

        // Публикуем событие
        publishOrderStatusChangedEvent(updatedOrder, previousStatus, reason);

        // Если был резервирован товар, отправляем сигнал для отмены
        if (previousStatus == OrderStatus.RESERVED) {
            publishInventoryCancellationEvent(orderId);
        }
    }

    @Transactional
    public void markEventAsProcessed(String eventId) {
        if (!processedEventRepository.existsById(eventId)) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(LocalDateTime.now())
                    .build());
        }
    }

    public boolean isEventProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void createTimeout(Long orderId, String expectedEvent) {
        OrderTimeout timeout = OrderTimeout.builder()
                .orderId(orderId)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMinutes(TIMEOUT_MINUTES)))
                .expectedEvent(expectedEvent)
                .build();
        timeoutRepository.save(timeout);
    }

    private void publishOrderCreatedEvent(Order order,
                                          java.util.List<OrderRequest.OrderItemRequest> items) {
        String userEmail = extractUserEmailFromSecurityContext();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId().toString())
                .userId(order.getUserId().toString())
                .userEmail(userEmail)
                .totalAmount(order.getTotalPrice())
                .items(items.stream()
                        .map(i -> OrderCreatedEvent.OrderItemDto.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .price(i.getUnitPrice())
                                .build())
                        .toList())
                .timestamp(System.currentTimeMillis())
                .build();

        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "order.created")
                .setHeader("kafka_messageKey", order.getId().toString())
                .build();

        kafkaTemplate.send(message);
        log.info("Published OrderCreatedEvent for order: {}", order.getId());
    }

    private void publishOrderStatusChangedEvent(Order order, OrderStatus previousStatus, String reason) {
        String userEmail = extractUserEmailFromSecurityContext();
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId(order.getId().toString())
                .userId(order.getUserId().toString())
                .userEmail(userEmail)
                .status(order.getStatus().name())
                .timestamp(System.currentTimeMillis())
                .build();

        Message<OrderStatusChangedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "order.status-changed")
                .setHeader("kafka_messageKey", order.getId().toString())
                .build();

        kafkaTemplate.send(message);
        log.info("Published OrderStatusChangedEvent for order: {}", order.getId());
    }

    private void publishInventoryCancellationEvent(Long orderId) {
        Message<Long> message = MessageBuilder
                .withPayload(orderId)
                .setHeader(KafkaHeaders.TOPIC, "inventory.release-reservation")
                .setHeader("kafka_messageKey", orderId.toString())
                .build();

        kafkaTemplate.send(message);
        log.info("Published inventory cancellation event for order: {}", orderId);
    }

    private String extractUserEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }

        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object email = map.get("email");
            return email != null ? email.toString() : "";
        }

        return "";
    }

    private BigDecimal calculateTotal(java.util.List<OrderRequest.OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .items(order.getItems().stream()
                        .map(item -> new com.orderplatform.order.dto.response.OrderItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTotalPrice()
                        ))
                        .toList())
                .totalPrice(order.getTotalPrice())
                .paymentId(order.getPaymentId())
                .shippingId(order.getShippingId())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .expectedDelivery(order.getExpectedDelivery())
                .build();
    }

    /**
     * Demo method: Simulate full order lifecycle (NEW → RESERVED → PAID)
     * For testing/demo purposes only.
     */
    @Transactional
    public OrderResponse simulateFullLifecycle(Long orderId) {
        log.info("[DEMO] Starting lifecycle simulation for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.NEW) {
            throw new InvalidStatusTransitionException(
                    "Demo lifecycle can only start from NEW status. Current: " + order.getStatus());
        }
        
        // Step 1: NEW → RESERVED
        order.setStatus(OrderStatus.RESERVED);
        orderRepository.save(order);
        log.info("[DEMO] Order {} transitioned to RESERVED", orderId);
        
        // Step 2: RESERVED → PAID
        order.setStatus(OrderStatus.PAID);
        order.setPaymentId("DEMO-PAY-" + System.currentTimeMillis());
        orderRepository.save(order);
        log.info("[DEMO] Order {} transitioned to PAID with paymentId: {}", orderId, order.getPaymentId());
        
        return mapToResponse(order);
    }
}