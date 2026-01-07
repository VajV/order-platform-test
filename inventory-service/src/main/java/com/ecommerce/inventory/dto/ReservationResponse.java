// ReservationResponse.java
package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.entity.InventoryReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for reservation result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long reservationId;
    private String orderId;
    private String productId;
    private Integer reservedQuantity;
    private InventoryReservation.ReservationStatus status;
    private boolean success;
    private String message;
    private LocalDateTime createdAt;
}
