package com.ecommerce.product.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {
    private String eventType;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private LocalDateTime timestamp;
    private String source;
}