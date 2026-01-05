package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "rating", target = "rating")
    @Mapping(source = "reviewCount", target = "reviewCount")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    Product toEntity(ProductRequest request);
}
