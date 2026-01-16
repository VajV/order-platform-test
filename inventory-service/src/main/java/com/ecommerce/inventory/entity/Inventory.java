package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private Long quantityAvailable = 0L;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Long quantityReserved = 0L;

    @Column(name = "reorder_level")
    private Long reorderLevel;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    /**
     * Получить доступное количество (не зарезервированное)
     */
    public Long getAvailableQuantity() {
        return quantityAvailable - quantityReserved;
    }

    /**
     * Проверить что доступно достаточно товара
     */
    public boolean hasAvailableQuantity(Long quantity) {
        return getAvailableQuantity() >= quantity;
    }

    /**
     * Зарезервировать товар
     */
    public void reserve(Long quantity) {
        if (!hasAvailableQuantity(quantity)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %d. Available: %d, Requested: %d",
                            productId, getAvailableQuantity(), quantity)
            );
        }
        this.quantityReserved += quantity;
    }

    /**
     * Освободить резерв (компенсация или подтверждение заказа)
     */
    public void release(Long quantity) {
        if (this.quantityReserved < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot release %d items. Only %d reserved for product %d",
                            quantity, this.quantityReserved, productId)
            );
        }
        this.quantityReserved -= quantity;
    }

    /**
     * Подтвердить заказ - уменьшить общее количество и освободить резерв
     */
    public void confirm(Long quantity) {
        if (this.quantityReserved < quantity) {
            throw new IllegalStateException("Reserved quantity is less than confirmation quantity");
        }
        this.quantityAvailable -= quantity;
        this.quantityReserved -= quantity;
    }

    /**
     * Добавить товар на склад
     */
    public void addStock(Long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantityAvailable += quantity;
        this.lastRestockedAt = LocalDateTime.now();
    }
}
