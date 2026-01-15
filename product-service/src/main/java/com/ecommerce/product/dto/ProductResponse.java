package com.ecommerce.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;  // ← String вместо Long

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String categoryId;  // ← String вместо Long

    private String categoryName;

    private String imageUrl;

    private Boolean active;

    private Double rating;

    private Integer reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
