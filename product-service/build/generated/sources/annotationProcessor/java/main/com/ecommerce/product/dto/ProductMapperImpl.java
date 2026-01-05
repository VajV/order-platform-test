package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-05T22:41:00+0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.5.jar, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        Long categoryId = null;
        String categoryName = null;
        Double rating = null;
        Integer reviewCount = null;
        Long id = null;
        String name = null;
        String description = null;
        BigDecimal price = null;
        Integer stock = null;
        String imageUrl = null;
        boolean active = false;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        categoryId = productCategoryId( product );
        categoryName = productCategoryName( product );
        rating = product.getRating();
        reviewCount = product.getReviewCount();
        id = product.getId();
        name = product.getName();
        description = product.getDescription();
        price = product.getPrice();
        stock = product.getStock();
        imageUrl = product.getImageUrl();
        active = product.isActive();
        createdAt = product.getCreatedAt();
        updatedAt = product.getUpdatedAt();

        ProductResponse productResponse = new ProductResponse( id, name, description, price, stock, categoryId, categoryName, imageUrl, rating, reviewCount, active, createdAt, updatedAt );

        return productResponse;
    }

    @Override
    public Product toEntity(ProductRequest request) {
        if ( request == null ) {
            return null;
        }

        Product.ProductBuilder product = Product.builder();

        product.name( request.getName() );
        product.description( request.getDescription() );
        product.price( request.getPrice() );
        product.stock( request.getStock() );
        product.imageUrl( request.getImageUrl() );
        product.active( request.isActive() );

        return product.build();
    }

    private Long productCategoryId(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        Long id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String productCategoryName(Product product) {
        if ( product == null ) {
            return null;
        }
        Category category = product.getCategory();
        if ( category == null ) {
            return null;
        }
        String name = category.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
