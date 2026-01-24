package com.orderplatform.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.event.InventoryFailedEvent;
import com.orderplatform.order.dto.event.InventoryReservedEvent;
import com.orderplatform.order.dto.event.PaymentCompletedEvent;
import com.orderplatform.order.dto.event.PaymentFailedEvent;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.service.OrderSagaOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для Saga-оркестрации заказов.
 * Проверяют реакцию на события inventory.reserved, inventory.failed, payment.completed, payment.failed.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "order.created", "order.status-changed", "order.cancelled",
                "order.payment-requested", "order.ready-to-ship",
                "inventory.reserved", "inventory.failed",
                "inventory.release-reservation"
        },
        brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@DisplayName("Order Saga Integration Tests")
class OrderSagaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_saga_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSagaOrchestrator sagaOrchestrator;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // ========== INVENTORY RESERVED TESTS ==========

    @Nested
    @DisplayName("On Inventory Reserved")
    class OnInventoryReservedTests {

        @Test
        @DisplayName("должен перевести заказ из NEW в RESERVED при получении inventory.reserved")
        void shouldTransitionToReservedOnInventoryReserved() {
            // Given: заказ в статусе NEW
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .orderId(order.getId())
                    .inventoryReservationId("res-123")
                    .build();

            // When: обрабатываем событие
            sagaOrchestrator.onInventoryReserved(event);

            // Then: статус должен измениться на RESERVED
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
        }

        @Test
        @DisplayName("не должен менять статус если заказ не в NEW")
        void shouldNotChangeStatusIfNotNew() {
            // Given: заказ уже в статусе RESERVED
            Order order = createAndSaveOrder(1L, OrderStatus.RESERVED);

            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .orderId(order.getId())
                    .inventoryReservationId("res-123")
                    .build();

            // When
            sagaOrchestrator.onInventoryReserved(event);

            // Then: статус не изменился
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
        }
    }

    // ========== INVENTORY FAILED TESTS ==========

    @Nested
    @DisplayName("On Inventory Failed")
    class OnInventoryFailedTests {

        @Test
        @DisplayName("должен отменить заказ при недостатке товара")
        void shouldCancelOrderOnInventoryFailed() {
            // Given: заказ в статусе NEW
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            InventoryFailedEvent event = InventoryFailedEvent.builder()
                    .orderId(order.getId())
                    .reason("Insufficient stock")
                    .failedProductId("prod-001")
                    .build();

            // When
            sagaOrchestrator.onInventoryFailed(event);

            // Then: заказ должен быть отменён
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updatedOrder.getCancellationReason()).contains("Insufficient stock");
        }
    }

    // ========== PAYMENT COMPLETED TESTS ==========

    @Nested
    @DisplayName("On Payment Completed")
    class OnPaymentCompletedTests {

        @Test
        @DisplayName("должен перевести заказ из RESERVED в PAID")
        void shouldTransitionToPaidOnPaymentCompleted() {
            // Given: заказ в статусе RESERVED
            Order order = createAndSaveOrder(1L, OrderStatus.RESERVED);

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(order.getId())
                    .paymentId("pay-123")
                    .amount(new BigDecimal("199.98"))
                    .build();

            // When
            sagaOrchestrator.onPaymentCompleted(event);

            // Then
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(updatedOrder.getPaymentId()).isEqualTo("pay-123");
        }
    }

    // ========== PAYMENT FAILED TESTS ==========

    @Nested
    @DisplayName("On Payment Failed")
    class OnPaymentFailedTests {

        @Test
        @DisplayName("должен отменить заказ и освободить резерв при неудачной оплате")
        void shouldCancelOrderAndReleaseReservationOnPaymentFailed() {
            // Given: заказ в статусе RESERVED
            Order order = createAndSaveOrder(1L, OrderStatus.RESERVED);

            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .orderId(order.getId())
                    .reason("Card declined")
                    .build();

            // When
            sagaOrchestrator.onPaymentFailed(event);

            // Then: заказ должен быть отменён
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updatedOrder.getCancellationReason()).contains("Card declined");
        }
    }

    // ========== FULL SAGA FLOW TEST ==========

    @Nested
    @DisplayName("Full Saga Flow")
    class FullSagaFlowTests {

        @Test
        @DisplayName("полный успешный сценарий: NEW → RESERVED → PAID")
        void shouldCompleteSuccessfulSagaFlow() {
            // 1. Создаём заказ в статусе NEW
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);
            Long orderId = order.getId();

            // 2. Получаем inventory.reserved → переход в RESERVED
            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderId(orderId)
                    .inventoryReservationId("res-123")
                    .build();
            sagaOrchestrator.onInventoryReserved(reservedEvent);

            Order afterReserved = orderRepository.findById(orderId).orElseThrow();
            assertThat(afterReserved.getStatus()).isEqualTo(OrderStatus.RESERVED);

            // 3. Получаем payment.completed → переход в PAID
            PaymentCompletedEvent paymentEvent = PaymentCompletedEvent.builder()
                    .orderId(orderId)
                    .paymentId("pay-456")
                    .amount(new BigDecimal("199.98"))
                    .build();
            sagaOrchestrator.onPaymentCompleted(paymentEvent);

            Order afterPaid = orderRepository.findById(orderId).orElseThrow();
            assertThat(afterPaid.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(afterPaid.getPaymentId()).isEqualTo("pay-456");
        }

        @Test
        @DisplayName("сценарий компенсации: NEW → CANCELLED (inventory failed)")
        void shouldCompensateOnInventoryFailure() {
            // 1. Создаём заказ
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            // 2. Получаем inventory.failed → компенсация
            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(order.getId())
                    .reason("Out of stock")
                    .failedProductId("prod-001")
                    .build();
            sagaOrchestrator.onInventoryFailed(failedEvent);

            // 3. Проверяем, что заказ отменён
            Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    // ========== HELPER METHODS ==========

    private Order createAndSaveOrder(Long userId, OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .status(status)
                .totalPrice(new BigDecimal("199.98"))
                .build();
        return orderRepository.save(order);
    }
}
