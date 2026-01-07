package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for Inventory persistence.
 * Includes pessimistic locking for critical stock updates.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Find inventory by product ID without locking.
     * Use for reads only.
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * Find inventory by product ID with PESSIMISTIC_WRITE lock.
     * Exclusively locks the row from first read until transaction end.
     * Other threads must wait until lock released.
     *
     * Use for:
     * - Stock reservations (prevent overselling)
     * - Inventory updates with guaranteed isolation
     *
     * @param productId the product ID
     * @return locked Inventory if exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") String productId);

    /**
     * Find inventory by product ID with PESSIMISTIC_READ lock.
     * Shared lock - multiple readers allowed, but prevents writes.
     * Lighter weight than PESSIMISTIC_WRITE.
     *
     * Use for:
     * - Reading current stock levels (no updates needed)
     *
     * @param productId the product ID
     * @return locked Inventory if exists
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForRead(@Param("productId") String productId);

    /**
     * Check if inventory exists for product.
     *
     * @param productId the product ID
     * @return true if inventory tracked, false otherwise
     */
    boolean existsByProductId(String productId);
}
