package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ✅ Автоматический маппинг (MapStruct сам найдёт одинаковые поля)
    ProductResponse toResponse(Product product);

    // ✅ Игнорируем только те поля, которые не нужно заполнять из request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductCreateRequest request);
}
