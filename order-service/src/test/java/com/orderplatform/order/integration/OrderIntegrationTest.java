package com.orderplatform.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.domain.entity.Order;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для Order API.
 * Используют Testcontainers (PostgreSQL) и EmbeddedKafka.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order.created", "order.status-changed", "order.cancelled", "inventory.reserved", "inventory.failed"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@DisplayName("Order API Integration Tests")
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_test_db")
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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // ========== CREATE ORDER TESTS ==========

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTests {

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен создать заказ и вернуть 201")
        void shouldCreateOrderSuccessfully() throws Exception {
            OrderRequest request = createValidOrderRequest();

            MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value("NEW"))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andReturn();

            OrderResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    OrderResponse.class
            );

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.98"));

            // Проверяем, что заказ сохранён в БД
            Order savedOrder = orderRepository.findById(response.getId()).orElseThrow();
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.NEW);
        }

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть 400 при пустом списке товаров")
        void shouldReturn400WhenItemsEmpty() throws Exception {
            OrderRequest request = OrderRequest.builder()
                    .userId(1L)
                    .items(List.of())
                    .build();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("должен вернуть 401 без аутентификации")
        void shouldReturn401WithoutAuth() throws Exception {
            OrderRequest request = createValidOrderRequest();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET ORDER TESTS ==========

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderTests {

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть заказ по ID")
        void shouldReturnOrderById() throws Exception {
            // Создаём заказ
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            mockMvc.perform(get("/api/orders/{id}", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть 404 для несуществующего заказа")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(get("/api/orders/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET MY ORDERS TESTS ==========

    @Nested
    @DisplayName("GET /api/orders/my")
    class GetMyOrdersTests {

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть заказы текущего пользователя")
        void shouldReturnCurrentUserOrders() throws Exception {
            // Создаём заказы для разных пользователей
            createAndSaveOrder(1L, OrderStatus.NEW);
            createAndSaveOrder(1L, OrderStatus.PAID);
            createAndSaveOrder(2L, OrderStatus.NEW); // Другой пользователь

            mockMvc.perform(get("/api/orders/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    // ========== UPDATE STATUS TESTS ==========

    @Nested
    @DisplayName("PUT /api/orders/{id}/status")
    class UpdateStatusTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("должен обновить статус заказа (ADMIN)")
        void shouldUpdateOrderStatus() throws Exception {
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                            .param("status", "RESERVED")
                            .param("reason", "Inventory reserved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESERVED"));

            // Проверяем в БД
            Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
        }

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть 403 для USER при обновлении статуса")
        void shouldReturn403ForUserRole() throws Exception {
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                            .param("status", "RESERVED"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== CANCEL ORDER TESTS ==========

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel")
    class CancelOrderTests {

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен отменить заказ в статусе NEW")
        void shouldCancelOrderInNewStatus() throws Exception {
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);

            mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                            .param("reason", "Changed my mind"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.cancellationReason").value("Changed my mind"));
        }

        @Test
        @WithMockUser(username = "1", roles = {"USER"})
        @DisplayName("должен вернуть ошибку при отмене COMPLETED заказа")
        void shouldFailToCancelCompletedOrder() throws Exception {
            Order order = createAndSaveOrder(1L, OrderStatus.COMPLETED);

            mockMvc.perform(post("/api/orders/{id}/cancel", order.getId())
                            .param("reason", "Too late"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== ORDER LIFECYCLE TESTS ==========

    @Nested
    @DisplayName("Order Lifecycle")
    class OrderLifecycleTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("заказ должен пройти полный жизненный цикл: NEW → RESERVED → PAID → SHIPPED → COMPLETED")
        void shouldCompleteFullOrderLifecycle() throws Exception {
            // 1. Создаём заказ
            Order order = createAndSaveOrder(1L, OrderStatus.NEW);
            Long orderId = order.getId();

            // 2. NEW → RESERVED
            mockMvc.perform(put("/api/orders/{id}/status", orderId)
                            .param("status", "RESERVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESERVED"));

            // 3. RESERVED → PAID
            mockMvc.perform(put("/api/orders/{id}/status", orderId)
                            .param("status", "PAID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"));

            // 4. PAID → SHIPPED
            mockMvc.perform(put("/api/orders/{id}/status", orderId)
                            .param("status", "SHIPPED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));

            // 5. SHIPPED → COMPLETED
            mockMvc.perform(put("/api/orders/{id}/status", orderId)
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            // Проверяем финальное состояние
            Order completedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("нельзя перейти из COMPLETED в другой статус")
        void shouldNotAllowTransitionFromCompleted() throws Exception {
            Order order = createAndSaveOrder(1L, OrderStatus.COMPLETED);

            mockMvc.perform(put("/api/orders/{id}/status", order.getId())
                            .param("status", "NEW"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== HELPER METHODS ==========

    private OrderRequest createValidOrderRequest() {
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId("prod-001");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("99.99"));

        return OrderRequest.builder()
                .userId(1L)
                .items(List.of(item))
                .build();
    }

    private Order createAndSaveOrder(Long userId, OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .status(status)
                .totalPrice(new BigDecimal("199.98"))
                .build();
        return orderRepository.save(order);
    }
}
