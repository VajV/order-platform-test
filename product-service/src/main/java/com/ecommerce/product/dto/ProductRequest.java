package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        private String name;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private BigDecimal price;

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be at least 0")
        private Integer stock;

        @NotNull(message = "Category ID is required")
        private String categoryId;  // ← String вместо Long

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        private String imageUrl;

        @Builder.Default
        private boolean active = true;
}
