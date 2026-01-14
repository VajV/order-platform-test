package com.orderplatform.order.dto.event;

import com.orderplatform.order.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChangedEvent {
    private Long orderId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String reason;
    private LocalDateTime timestamp;
}