package com.ecommerce.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {

    private String eventType;  // CREATED, UPDATED, DELETED, STOCK_UPDATED

    private String productId;  // ← String вместо Long (для MongoDB)

    private String productName;

    private BigDecimal price;

    private Integer stock;

    private String status;  // active, inactive, deleted

    private LocalDateTime timestamp;

    private String source;  // product-service
}
