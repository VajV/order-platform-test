package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // ← ИСПРАВЛЕНО: UUID вместо IDENTITY
    private UUID id;  // ← ИСПРАВЛЕНО: UUID вместо Long

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;  // ← ИСПРАВЛЕНО: UUID вместо String

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    @Builder.Default
    private Long version = 0L;  // ← ИСПРАВЛЕНО: Long вместо Integer (для оптимистичных блокировок)

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Получить доступное количество (не зарезервированное)
     */
    public Integer getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    /**
     * Проверить что доступно достаточно товара
     */
    public boolean hasAvailableQuantity(Integer quantity) {
        return getAvailableQuantity() >= quantity;
    }

    /**
     * Зарезервировать товар
     */
    public void reserve(Integer quantity) {
        if (!hasAvailableQuantity(quantity)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            productId, getAvailableQuantity(), quantity)
            );
        }
        this.reservedQuantity += quantity;
    }

    /**
     * Освободить резерв (компенсация или подтверждение заказа)
     */
    public void release(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot release %d items. Only %d reserved for product %s",
                            quantity, this.reservedQuantity, productId)
            );
        }
        this.reservedQuantity -= quantity;
    }

    /**
     * Подтвердить заказ - уменьшить общее количество и освободить резерв
     */
    public void confirm(Integer quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Reserved quantity is less than confirmation quantity");
        }
        this.totalQuantity -= quantity;
        this.reservedQuantity -= quantity;
    }

    /**
     * Добавить товар на склад
     */
    public void addStock(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.totalQuantity += quantity;
    }
}
