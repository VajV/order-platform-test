package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        String categoryName,
        String imageUrl,
        Double rating,
        Integer reviewCount,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
