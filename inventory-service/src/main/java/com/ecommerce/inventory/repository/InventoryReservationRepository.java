package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InventoryReservation persistence.
 * Supports audit trails and saga pattern compensation.
 */
@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    /**
     * Find reservation by order ID.
     * Assuming one reservation per order (MVP).
     *
     * @param orderId the order ID
     * @return reservation if exists
     */
    Optional<InventoryReservation> findByOrderId(String orderId);

    /**
     * Find all pending reservations for an inventory.
     * Used for:
     * - Auditing current holds
     * - Calculating effectively reserved quantity
     *
     * @param inventoryId the inventory ID
     * @return list of pending reservations
     */
    @Query("SELECT r FROM InventoryReservation r " +
            "WHERE r.inventoryId = :inventoryId " +
            "AND r.status = 'PENDING'")
    List<InventoryReservation> findPendingByInventoryId(@Param("inventoryId") Long inventoryId);

    /**
     * Find reservations by order IDs (batch operation).
     * Useful for event processing.
     *
     * @param orderIds list of order IDs
     * @return list of reservations
     */
    List<InventoryReservation> findByOrderIdIn(List<String> orderIds);
}
