package com.orderplatform.order.dto.event;

import com.orderplatform.order.dto.request.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private List<OrderRequest.OrderItemRequest> items;
    private BigDecimal totalPrice;
    private LocalDateTime timestamp;
}
