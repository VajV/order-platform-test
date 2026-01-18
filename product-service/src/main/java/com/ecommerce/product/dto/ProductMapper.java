package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ✅ Автоматический маппинг (MapStruct сам найдёт одинаковые поля)
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toResponse(Product product);

    // ✅ Игнорируем только те поля, которые не нужно заполнять из request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "rating", constant = "0.0")
    @Mapping(target = "reviewCount", constant = "0")
    Product toEntity(ProductRequest request);
}
