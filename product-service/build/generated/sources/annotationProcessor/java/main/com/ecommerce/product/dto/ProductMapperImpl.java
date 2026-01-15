package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-15T17:12:30+0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.5.jar, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductResponse.ProductResponseBuilder productResponse = ProductResponse.builder();

        productResponse.id( product.getId() );
        productResponse.name( product.getName() );
        productResponse.description( product.getDescription() );
        productResponse.price( product.getPrice() );
        productResponse.stock( product.getStock() );
        productResponse.categoryId( product.getCategoryId() );
        productResponse.categoryName( product.getCategoryName() );
        productResponse.imageUrl( product.getImageUrl() );
        productResponse.active( product.getActive() );
        productResponse.rating( product.getRating() );
        productResponse.reviewCount( product.getReviewCount() );
        productResponse.createdAt( product.getCreatedAt() );
        productResponse.updatedAt( product.getUpdatedAt() );

        return productResponse.build();
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
        product.categoryId( request.getCategoryId() );
        product.imageUrl( request.getImageUrl() );
        product.active( request.isActive() );

        product.rating( (double) 0.0 );
        product.reviewCount( 0 );

        return product.build();
    }
}
