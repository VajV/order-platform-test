package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InventoryException;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        log.info("Creating inventory for product: {}", request.getProductId());

        // ✅ ИСПРАВЛЕНО: Преобразуем String -> UUID
        UUID productId = UUID.fromString(request.getProductId());

        if (inventoryRepository.existsByProductId(productId)) {
            throw new InventoryException(
                    String.format("Inventory already exists for product %s", productId),
                    "INVENTORY_ALREADY_EXISTS"
            );
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)  // ✅ Теперь UUID
                .totalQuantity(request.getTotalQuantity())
                .reservedQuantity(0)
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory created successfully for product: {} with id: {}",
                productId, saved.getId());

        return inventoryMapper.toResponse(saved);
    }

    public InventoryResponse getInventory(String productId) {
        log.debug("Fetching inventory for product: {}", productId);

        UUID uuid = UUID.fromString(productId);  // ✅ String -> UUID

        return inventoryRepository.findByProductId(uuid)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new InventoryException(
                        String.format("Inventory not found for product %s", productId),
                        "INVENTORY_NOT_FOUND"
                ));
    }

    @Transactional
    public Inventory getInventoryLockedForUpdate(String productId) {
        UUID uuid = UUID.fromString(productId);  // ✅ String -> UUID

        return inventoryRepository.findByProductIdWithLock(uuid)
                .orElseThrow(() -> new InventoryException(
                        String.format("Inventory not found for product %s", productId),
                        "INVENTORY_NOT_FOUND"
                ));
    }

    @Transactional
    public InventoryResponse updateTotalQuantity(String productId, Integer newQuantity) {
        log.info("Updating inventory quantity for product: {} to: {}", productId, newQuantity);

        Inventory inventory = getInventoryLockedForUpdate(productId);
        inventory.setTotalQuantity(newQuantity);
        Inventory updated = inventoryRepository.save(inventory);

        log.info("Inventory updated for product: {}", productId);
        return inventoryMapper.toResponse(updated);
    }
}
