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

    private String productId;

    private String productName;

    private BigDecimal price;

    private String status;  // published, unpublished, deleted

    private LocalDateTime timestamp;

    private String source;  // product-service
}
