package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks inventory reservations for orders.
 * Provides audit trail and supports saga pattern compensation.
 */
@Entity
@Table(name = "inventory_reservations",  // ← ИСПРАВЛЕНО: inventory_reservations вместо inventory_reservation
        indexes = {
                @Index(name = "idx_reservations_order_id", columnList = "order_id"),
                @Index(name = "idx_reservations_inventory_id", columnList = "inventory_id"),
                @Index(name = "idx_reservations_status", columnList = "status"),
                @Index(name = "idx_reservations_expires_at", columnList = "expires_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // ← ИСПРАВЛЕНО: UUID вместо IDENTITY
    private UUID id;  // ← ИСПРАВЛЕНО: UUID вместо Long

    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;  // ← ИСПРАВЛЕНО: UUID вместо Long

    @Column(name = "order_id", nullable = false)
    private UUID orderId;  // ← ИСПРАВЛЕНО: UUID вместо String

    @Column(name = "product_id", nullable = false)
    private UUID productId;  // ← ДОБАВЛЕНО: как в миграции

    @Column(name = "quantity", nullable = false)
    private Integer quantity;  // ← ИСПРАВЛЕНО: quantity вместо reservedQuantity

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;  // ← ИСПРАВЛЕНО: RESERVED вместо PENDING

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;  // ← ДОБАВЛЕНО: как в миграции

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;  // ← ОПЦИОНАЛЬНО (можно оставить для бизнес-логики)

    @Column(name = "released_at")
    private LocalDateTime releasedAt;  // ← ОПЦИОНАЛЬНО

    @Column(name = "failure_reason", length = 500)
    private String failureReason;  // ← ОПЦИОНАЛЬНО

    /**
     * Reservation status lifecycle:
     * RESERVED -> CONFIRMED (order paid)
     * RESERVED -> RELEASED (order cancelled/timeout)
     */
    public enum ReservationStatus {
        RESERVED,     // ← ИСПРАВЛЕНО: как в CHECK constraint миграции
        CONFIRMED,
        RELEASED
    }
}
