package com.ecommerce.product.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;  // MongoDB использует String для ID

    @Indexed
    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("price")
    private BigDecimal price;

    @Field("stock")
    @Builder.Default
    private Integer stock = 0;

    // Вместо @ManyToOne используем просто categoryId
    @Indexed
    @Field("category_id")
    private String categoryId;

    @Field("category_name")
    private String categoryName;  // Денормализация для быстрого доступа

    @Field("image_url")
    private String imageUrl;

    @Indexed
    @Field("active")
    @Builder.Default
    private Boolean active = true;

    @Field("rating")
    @Builder.Default
    private Double rating = 0.0;

    @Field("review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
