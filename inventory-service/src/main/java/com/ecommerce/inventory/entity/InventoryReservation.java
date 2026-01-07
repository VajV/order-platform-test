package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks inventory reservations for orders.
 * Provides audit trail and supports saga pattern compensation.
 */
@Entity
@Table(name = "inventory_reservation", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_inventory_status", columnList = "inventory_id, status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long inventoryId;

    @Column(nullable = false, length = 255)
    private String orderId;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime releasedAt;

    @Column(length = 500)
    private String failureReason;

    /**
     * Reservation status lifecycle:
     * PENDING -> CONFIRMED (order paid)
     * PENDING -> FAILED (insufficient stock)
     * CONFIRMED -> RELEASED (order cancelled/returned)
     */
    public enum ReservationStatus {
        PENDING,      // Initial state, temporary hold
        CONFIRMED,    // Order payment confirmed, hold becomes permanent
        FAILED,       // Reservation failed, no longer held
        RELEASED      // Reservation released (refund/return)
    }
}
