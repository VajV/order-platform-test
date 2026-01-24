package com.orderplatform.order.service;

import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.entity.OrderItem;
import com.orderplatform.order.domain.entity.OrderTimeout;
import com.orderplatform.order.domain.entity.ProcessedEvent;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.exception.OrderNotFoundException;
import com.orderplatform.order.exception.InvalidStatusTransitionException;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.repository.OrderTimeoutRepository;
import com.orderplatform.order.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для OrderService.
 * Используем MockitoExtension для изоляции от внешних зависимостей.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTimeoutRepository timeoutRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderRequest.OrderItemRequest testItemRequest;

    @BeforeEach
    void setUp() {
        // Подготовка тестовых данных
        testItemRequest = new OrderRequest.OrderItemRequest();
        testItemRequest.setProductId("507f1f77bcf86cd799439011");
        testItemRequest.setProductName("Test Product");
        testItemRequest.setQuantity(2);
        testItemRequest.setUnitPrice(new BigDecimal("99.99"));

        testOrderRequest = OrderRequest.builder()
                .userId(1L)
                .items(List.of(testItemRequest))
                .build();

        OrderItem testItem = OrderItem.builder()
                .id(1L)
                .productId("507f1f77bcf86cd799439011")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("99.99"))
                .totalPrice(new BigDecimal("199.98"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.NEW)
                .items(Set.of(testItem))
                .totalPrice(new BigDecimal("199.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== CREATE ORDER TESTS ==========

    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("должен успешно создать заказ")
        void shouldCreateOrderSuccessfully() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(timeoutRepository.save(any(OrderTimeout.class))).thenReturn(mock(OrderTimeout.class));
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            OrderResponse response = orderService.createOrder(testOrderRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.98"));

            // Verify interactions
            verify(orderRepository).save(any(Order.class));
            verify(timeoutRepository).save(any(OrderTimeout.class));
            verify(kafkaTemplate).send(any(Message.class));
        }

        @Test
        @DisplayName("должен корректно рассчитать сумму заказа")
        void shouldCalculateTotalPriceCorrectly() {
            // Given
            OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest();
            item1.setProductId("507f1f77bcf86cd799439011");
            item1.setProductName("Product 1");
            item1.setQuantity(2);
            item1.setUnitPrice(new BigDecimal("50.00"));

            OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest();
            item2.setProductId("507f1f77bcf86cd799439012");
            item2.setProductName("Product 2");
            item2.setQuantity(3);
            item2.setUnitPrice(new BigDecimal("30.00"));

            OrderRequest request = OrderRequest.builder()
                    .userId(1L)
                    .items(List.of(item1, item2))
                    .build();

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(1L);
                return o;
            });
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            orderService.createOrder(request);

            // Then
            Order savedOrder = orderCaptor.getValue();
            // 2 * 50 + 3 * 30 = 100 + 90 = 190
            assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("190.00"));
        }

        @Test
        @DisplayName("должен опубликовать событие order.created в Kafka")
        void shouldPublishOrderCreatedEvent() {
            // Given
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            orderService.createOrder(testOrderRequest);

            // Then
            verify(kafkaTemplate, times(1)).send(any(Message.class));
        }
    }

    // ========== GET ORDER TESTS ==========

    @Nested
    @DisplayName("getOrder()")
    class GetOrderTests {

        @Test
        @DisplayName("должен вернуть заказ по ID")
        void shouldReturnOrderById() {
            // Given
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // When
            OrderResponse response = orderService.getOrder(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.NEW);
        }

        @Test
        @DisplayName("должен выбросить OrderNotFoundException если заказ не найден")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrder(999L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Order not found: 999");
        }
    }

    // ========== GET ORDERS BY USER TESTS ==========

    @Nested
    @DisplayName("getOrdersByUser()")
    class GetOrdersByUserTests {

        @Test
        @DisplayName("должен вернуть страницу заказов пользователя")
        void shouldReturnPageOfUserOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);
            when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);

            // When
            Page<OrderResponse> response = orderService.getOrdersByUser(1L, pageable);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("должен вернуть пустую страницу если заказов нет")
        void shouldReturnEmptyPageWhenNoOrders() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(orderRepository.findByUserId(999L, pageable)).thenReturn(emptyPage);

            // When
            Page<OrderResponse> response = orderService.getOrdersByUser(999L, pageable);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalElements()).isEqualTo(0);
            assertThat(response.getContent()).isEmpty();
        }
    }

    // ========== UPDATE ORDER STATUS TESTS ==========

    @Nested
    @DisplayName("updateOrderStatus()")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("должен обновить статус с NEW на RESERVED")
        void shouldUpdateStatusFromNewToReserved() {
            // Given
            Order newOrder = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .items(Set.of())
                    .totalPrice(BigDecimal.TEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(newOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.RESERVED, "Inventory reserved");

            // Then
            assertThat(response.getStatus()).isEqualTo(OrderStatus.RESERVED);
            verify(kafkaTemplate).send(any(Message.class));
        }

        @Test
        @DisplayName("должен выбросить InvalidStatusTransitionException при недопустимом переходе")
        void shouldThrowExceptionOnInvalidTransition() {
            // Given
            Order completedOrder = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .status(OrderStatus.COMPLETED)
                    .items(Set.of())
                    .totalPrice(BigDecimal.TEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(completedOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.NEW, "Test"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Cannot transition from COMPLETED to NEW");
        }
    }

    // ========== CANCEL ORDER TESTS ==========

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrderTests {

        @Test
        @DisplayName("должен отменить заказ в статусе NEW")
        void shouldCancelOrderInNewStatus() {
            // Given
            Order newOrder = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .items(Set.of())
                    .totalPrice(BigDecimal.TEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(newOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            orderService.cancelOrder(1L, "Customer request");

            // Then
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(savedOrder.getCancellationReason()).isEqualTo("Customer request");
        }

        @Test
        @DisplayName("должен выбросить исключение при отмене COMPLETED заказа")
        void shouldThrowExceptionWhenCancellingCompletedOrder() {
            // Given
            Order completedOrder = Order.builder()
                    .id(1L)
                    .status(OrderStatus.COMPLETED)
                    .items(Set.of())
                    .totalPrice(BigDecimal.TEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(completedOrder));

            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, "Test"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Cannot cancel order in status: COMPLETED");
        }

        @Test
        @DisplayName("должен отправить событие отмены резервирования для RESERVED заказа")
        void shouldPublishInventoryCancellationForReservedOrder() {
            // Given
            Order reservedOrder = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .status(OrderStatus.RESERVED)
                    .items(Set.of())
                    .totalPrice(BigDecimal.TEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservedOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

            // When
            orderService.cancelOrder(1L, "Customer request");

            // Then
            // Должно быть 2 события: status-changed и inventory.release-reservation
            verify(kafkaTemplate, times(2)).send(any(Message.class));
        }
    }

    // ========== IDEMPOTENCY TESTS ==========

    @Nested
    @DisplayName("Event Processing Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("должен пометить событие как обработанное")
        void shouldMarkEventAsProcessed() {
            // Given
            String eventId = "event-123";
            when(processedEventRepository.existsById(eventId)).thenReturn(false);

            // When
            orderService.markEventAsProcessed(eventId);

            // Then
            verify(processedEventRepository).save(argThat(event ->
                    event.getEventId().equals(eventId)
            ));
        }

        @Test
        @DisplayName("не должен повторно сохранять уже обработанное событие")
        void shouldNotSaveDuplicateEvent() {
            // Given
            String eventId = "event-123";
            when(processedEventRepository.existsById(eventId)).thenReturn(true);

            // When
            orderService.markEventAsProcessed(eventId);

            // Then
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("должен возвращать true для обработанного события")
        void shouldReturnTrueForProcessedEvent() {
            // Given
            when(processedEventRepository.existsById("event-123")).thenReturn(true);

            // When
            boolean result = orderService.isEventProcessed("event-123");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("должен возвращать false для необработанного события")
        void shouldReturnFalseForUnprocessedEvent() {
            // Given
            when(processedEventRepository.existsById("event-456")).thenReturn(false);

            // When
            boolean result = orderService.isEventProcessed("event-456");

            // Then
            assertThat(result).isFalse();
        }
    }
}
