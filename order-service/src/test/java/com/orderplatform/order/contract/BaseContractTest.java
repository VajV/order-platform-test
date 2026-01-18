package com.orderplatform.order.contract;

import com.orderplatform.order.controller.OrderController;
import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.response.OrderItemResponse;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.service.OrderService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Базовый класс для Contract Tests.
 * Spring Cloud Contract будет генерировать тесты, наследующие этот класс.
 * 
 * Контракты определяют API между order-service и его consumers:
 * - api-gateway (REST calls)
 * - frontend applications
 * - other microservices
 */
@WebMvcTest(OrderController.class)
@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "USER")
public abstract class BaseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        setupMocks();
    }

    /**
     * Настройка моков для контрактных тестов.
     * Эти моки обеспечивают предсказуемое поведение для всех контрактов.
     */
    private void setupMocks() {
        // Mock для getOrder
        when(orderService.getOrder(anyLong())).thenReturn(createSampleOrder());
        
        // Mock для createOrder
        when(orderService.createOrder(any())).thenReturn(createSampleOrder());
        
        // Mock для getUserOrders
        when(orderService.getOrdersByUser(anyLong(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(createSampleOrder())
                ));
    }

    /**
     * Создаёт стандартный объект заказа для контрактных тестов.
     * ВАЖНО: Эти данные должны соответствовать контрактам в contracts/*.groovy
     */
    protected OrderResponse createSampleOrder() {
        return OrderResponse.builder()
                .id(1L)
                .userId(100L)
                .status(OrderStatus.NEW)
                .items(List.of(
                        new OrderItemResponse(
                                1L,
                                "PROD-001",
                                "Test Product",
                                2,
                                new BigDecimal("99.99"),
                                new BigDecimal("199.98")
                        )
                ))
                .totalPrice(new BigDecimal("199.98"))
                .createdAt(LocalDateTime.of(2026, 1, 19, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 19, 10, 0, 0))
                .build();
    }
}

