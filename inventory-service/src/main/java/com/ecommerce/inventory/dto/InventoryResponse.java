// InventoryResponse.java
package com.ecommerce.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for inventory information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private Long id;
    private String productId;
    private Integer totalQuantity;
    private Integer reservedQuantity;

    @JsonProperty("availableQuantity")
    private Integer availableQuantity;

    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
