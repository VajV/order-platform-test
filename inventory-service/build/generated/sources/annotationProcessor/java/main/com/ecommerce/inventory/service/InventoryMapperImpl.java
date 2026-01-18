package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-18T04:33:22+0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.5.jar, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class InventoryMapperImpl implements InventoryMapper {

    @Override
    public InventoryResponse toResponse(Inventory inventory) {
        if ( inventory == null ) {
            return null;
        }

        InventoryResponse.InventoryResponseBuilder inventoryResponse = InventoryResponse.builder();

        inventoryResponse.id( inventory.getId() );
        inventoryResponse.productId( inventory.getProductId() );
        inventoryResponse.quantityAvailable( inventory.getQuantityAvailable() );
        inventoryResponse.quantityReserved( inventory.getQuantityReserved() );
        inventoryResponse.reorderLevel( inventory.getReorderLevel() );
        inventoryResponse.warehouseLocation( inventory.getWarehouseLocation() );
        inventoryResponse.lastRestockedAt( inventory.getLastRestockedAt() );
        inventoryResponse.createdAt( inventory.getCreatedAt() );
        inventoryResponse.updatedAt( inventory.getUpdatedAt() );

        inventoryResponse.availableQuantity( inventory.getAvailableQuantity() );

        return inventoryResponse.build();
    }
}
