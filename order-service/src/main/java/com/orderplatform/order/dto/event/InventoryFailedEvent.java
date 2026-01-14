package com.orderplatform.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryFailedEvent {
    private Long orderId;
    private String reason;
    private String failedProductId;
}