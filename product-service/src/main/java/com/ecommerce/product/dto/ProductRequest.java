package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

        @NotBlank(message = "Product name is required")
        @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
        private String name;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        private BigDecimal price;

        @NotNull(message = "Stock is required")
        @PositiveOrZero(message = "Stock must be positive or zero")
        private Integer stock;

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be positive")
        private Long categoryId;

        private String imageUrl;

        @Builder.Default
        private boolean active = true;
}