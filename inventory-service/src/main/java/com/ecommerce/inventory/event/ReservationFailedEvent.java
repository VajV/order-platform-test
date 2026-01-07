package com.ecommerce.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationFailedEvent {
    private String orderId;
    private String productId;
    private Integer requestedQuantity;
    private String reason;
    private String timestamp;
}
