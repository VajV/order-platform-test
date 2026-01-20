package com.orderplatform.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для Order Service.
 * Тестирует полный цикл создания и обработки заказов с реальной БД и Kafka.
 * 
 * Требования: Docker должен быть запущен.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@WithMockUser(roles = "USER")
@DisplayName("Order Service Integration Tests")
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_order_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        // Отключаем Vault
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // ========== CREATE ORDER TESTS ==========

    @Nested
    @DisplayName("POST /api/v1/orders - Create Order")
    class CreateOrderTests {

        @Test
        @DisplayName("должен создать заказ и сохранить в БД")
        void shouldCreateOrderAndSaveToDatabase() throws Exception {
            // Given
            OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
            item.setProductId("PROD-001");
            item.setProductName("Test Product");
            item.setQuantity(2);
            item.setUnitPrice(new BigDecimal("99.99"));

            OrderRequest request = OrderRequest.builder()
                    .userId(1L)
                    .items(List.of(item))
                    .build();

            // When
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("NEW"))
                    .andExpect(jsonPath("$.totalPrice").value(199.98));

            // Then - проверяем что заказ сохранён в БД
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Order> orders = orderRepository.findAll();
                assertThat(orders).hasSize(1);
                assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.NEW);
                assertThat(orders.get(0).getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.98"));
            });
        }

        @Test
        @DisplayName("должен отклонить заказ без товаров")
        void shouldRejectOrderWithoutItems() throws Exception {
            // Given
            OrderRequest request = OrderRequest.builder()
                    .userId(1L)
                    .items(List.of())
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("должен корректно рассчитать сумму для нескольких товаров")
        void shouldCalculateTotalForMultipleItems() throws Exception {
            // Given
            OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest();
            item1.setProductId("PROD-001");
            item1.setProductName("Product 1");
            item1.setQuantity(2);
            item1.setUnitPrice(new BigDecimal("50.00"));

            OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest();
            item2.setProductId("PROD-002");
            item2.setProductName("Product 2");
            item2.setQuantity(3);
            item2.setUnitPrice(new BigDecimal("30.00"));

            OrderRequest request = OrderRequest.builder()
                    .userId(1L)
                    .items(List.of(item1, item2))
                    .build();

            // When & Then
            // 2 * 50 + 3 * 30 = 100 + 90 = 190
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalPrice").value(190.00));
        }
    }

    // ========== GET ORDER TESTS ==========

    @Nested
    @DisplayName("GET /api/v1/orders/{id} - Get Order")
    class GetOrderTests {

        @Test
        @DisplayName("должен вернуть существующий заказ")
        void shouldReturnExistingOrder() throws Exception {
            // Given - создаём заказ напрямую в БД
            Order order = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .totalPrice(new BigDecimal("100.00"))
                    .build();
            Order savedOrder = orderRepository.save(order);

            // When & Then
            mockMvc.perform(get("/api/v1/orders/{id}", savedOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedOrder.getId()))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        @DisplayName("должен вернуть 404 для несуществующего заказа")
        void shouldReturn404ForNonExistingOrder() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== UPDATE ORDER STATUS TESTS ==========

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status - Update Status")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("должен обновить статус заказа с NEW на RESERVED")
        void shouldUpdateStatusFromNewToReserved() throws Exception {
            // Given
            Order order = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .totalPrice(new BigDecimal("100.00"))
                    .build();
            Order savedOrder = orderRepository.save(order);

            // When & Then
            mockMvc.perform(patch("/api/v1/orders/{id}/status", savedOrder.getId())
                            .param("status", "RESERVED")
                            .param("reason", "Inventory reserved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESERVED"));

            // Verify in DB
            Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
        }

        @Test
        @DisplayName("должен отклонить недопустимый переход статуса")
        void shouldRejectInvalidStatusTransition() throws Exception {
            // Given - заказ уже COMPLETED
            Order order = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.COMPLETED)
                    .totalPrice(new BigDecimal("100.00"))
                    .build();
            Order savedOrder = orderRepository.save(order);

            // When & Then - пытаемся перевести в NEW
            mockMvc.perform(patch("/api/v1/orders/{id}/status", savedOrder.getId())
                            .param("status", "NEW")
                            .param("reason", "Test"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== CANCEL ORDER TESTS ==========

    @Nested
    @DisplayName("DELETE /api/v1/orders/{id} - Cancel Order")
    class CancelOrderTests {

        @Test
        @DisplayName("должен отменить заказ")
        void shouldCancelOrder() throws Exception {
            // Given
            Order order = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .totalPrice(new BigDecimal("100.00"))
                    .build();
            Order savedOrder = orderRepository.save(order);

            // When & Then
            mockMvc.perform(delete("/api/v1/orders/{id}", savedOrder.getId())
                            .param("reason", "Customer request"))
                    .andExpect(status().isNoContent());

            // Verify in DB
            Order cancelledOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(cancelledOrder.getCancellationReason()).isEqualTo("Customer request");
        }
    }

    // ========== GET USER ORDERS TESTS ==========

    @Nested
    @DisplayName("GET /api/v1/orders/user/{userId} - Get User Orders")
    class GetUserOrdersTests {

        @Test
        @DisplayName("должен вернуть заказы пользователя")
        void shouldReturnUserOrders() throws Exception {
            // Given - создаём несколько заказов для пользователя
            Order order1 = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.NEW)
                    .totalPrice(new BigDecimal("100.00"))
                    .build();
            Order order2 = Order.builder()
                    .userId(1L)
                    .status(OrderStatus.COMPLETED)
                    .totalPrice(new BigDecimal("200.00"))
                    .build();
            Order order3 = Order.builder()
                    .userId(2L) // другой пользователь
                    .status(OrderStatus.NEW)
                    .totalPrice(new BigDecimal("300.00"))
                    .build();

            orderRepository.saveAll(List.of(order1, order2, order3));

            // When & Then
            mockMvc.perform(get("/api/v1/orders/user/{userId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].userId").value(org.hamcrest.Matchers.everyItem(
                            org.hamcrest.Matchers.equalTo(1)
                    )));
        }
    }
}

