package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {  // ✅ String ID

    Optional<InventoryReservation> findByOrderId(String orderId);  // ✅ String

    @Query("SELECT r FROM InventoryReservation r " +
            "WHERE r.inventoryId = :inventoryId " +
            "AND r.status = 'RESERVED'")  // ✅ RESERVED вместо PENDING
    List<InventoryReservation> findPendingByInventoryId(@Param("inventoryId") String inventoryId);  // ✅ String

    List<InventoryReservation> findByOrderIdIn(List<String> orderIds);  // ✅ String
}
