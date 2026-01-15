package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations",
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;  // ✅ String (UUID as String)

    @Column(name = "inventory_id", nullable = false)
    private String inventoryId;  // ✅ String

    @Column(name = "order_id", nullable = false)
    private String orderId;  // ✅ String

    @Column(name = "product_id", nullable = false)
    private String productId;  // ✅ String

    @Column(name = "quantity", nullable = false)
    private Integer quantity;  // ✅ quantity (не reservedQuantity)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public enum ReservationStatus {
        RESERVED,
        CONFIRMED,
        RELEASED
    }
}
