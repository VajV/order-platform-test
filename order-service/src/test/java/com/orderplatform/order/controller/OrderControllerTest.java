package com.orderplatform.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.dto.response.OrderItemResponse;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.exception.InvalidStatusTransitionException;
import com.orderplatform.order.exception.OrderNotFoundException;
import com.orderplatform.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для OrderController.
 * Использует @WebMvcTest для тестирования REST API без полного контекста Spring.
 */
@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderResponse createTestOrderResponse() {
        return OrderResponse.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.NEW)
                .items(List.of(new OrderItemResponse(
                        1L, "PROD-001", "Test Product", 2,
                        new BigDecimal("99.99"), new BigDecimal("199.98")
                )))
                .totalPrice(new BigDecimal("199.98"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== CREATE ORDER TESTS ==========

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrderTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен создать заказ и вернуть 201")
        void shouldCreateOrderAndReturn201() throws Exception {
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

            when(orderService.createOrder(any(OrderRequest.class)))
                    .thenReturn(createTestOrderResponse());

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("NEW"))
                    .andExpect(jsonPath("$.totalPrice").value(199.98));

            verify(orderService).createOrder(any(OrderRequest.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен вернуть 400 при невалидном запросе")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given - пустой запрос
            OrderRequest request = OrderRequest.builder()
                    .userId(null)
                    .items(null)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("должен вернуть 401 без аутентификации")
        void shouldReturn401WithoutAuthentication() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET ORDER TESTS ==========

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен вернуть заказ по ID")
        void shouldReturnOrderById() throws Exception {
            // Given
            when(orderService.getOrder(1L)).thenReturn(createTestOrderResponse());

            // When & Then
            mockMvc.perform(get("/api/v1/orders/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен вернуть 404 если заказ не найден")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            // Given
            when(orderService.getOrder(999L))
                    .thenThrow(new OrderNotFoundException("Order not found: 999"));

            // When & Then
            mockMvc.perform(get("/api/v1/orders/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET USER ORDERS TESTS ==========

    @Nested
    @DisplayName("GET /api/v1/orders?userId={userId}")
    class GetUserOrdersTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен вернуть заказы пользователя")
        void shouldReturnUserOrders() throws Exception {
            // Given
            Page<OrderResponse> page = new PageImpl<>(List.of(createTestOrderResponse()));
            when(orderService.getOrdersByUser(eq(1L), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].userId").value(1));
        }
    }

    // ========== UPDATE ORDER STATUS TESTS ==========

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status")
    class UpdateOrderStatusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("должен обновить статус заказа")
        void shouldUpdateOrderStatus() throws Exception {
            // Given
            OrderResponse updatedOrder = OrderResponse.builder()
                    .id(1L)
                    .userId(1L)
                    .status(OrderStatus.RESERVED)
                    .totalPrice(new BigDecimal("199.98"))
                    .build();

            when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.RESERVED), anyString()))
                    .thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                            .with(csrf())
                            .param("status", "RESERVED")
                            .param("reason", "Inventory reserved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESERVED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("должен вернуть 400 при недопустимом переходе статуса")
        void shouldReturn400ForInvalidStatusTransition() throws Exception {
            // Given
            when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.NEW), anyString()))
                    .thenThrow(new InvalidStatusTransitionException("Cannot transition from COMPLETED to NEW"));

            // When & Then
            mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                            .with(csrf())
                            .param("status", "NEW")
                            .param("reason", "Test"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== CANCEL ORDER TESTS ==========

    @Nested
    @DisplayName("DELETE /api/v1/orders/{id}")
    class CancelOrderTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен отменить заказ")
        void shouldCancelOrder() throws Exception {
            // Given
            doNothing().when(orderService).cancelOrder(eq(1L), anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/orders/{id}", 1L)
                            .with(csrf())
                            .param("reason", "Customer request"))
                    .andExpect(status().isNoContent());

            verify(orderService).cancelOrder(1L, "Customer request");
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("должен вернуть 404 при отмене несуществующего заказа")
        void shouldReturn404WhenCancellingNonExistingOrder() throws Exception {
            // Given
            doThrow(new OrderNotFoundException("Order not found: 999"))
                    .when(orderService).cancelOrder(eq(999L), anyString());

            // When & Then
            mockMvc.perform(delete("/api/v1/orders/{id}", 999L)
                            .with(csrf())
                            .param("reason", "Test"))
                    .andExpect(status().isNotFound());
        }
    }
}

