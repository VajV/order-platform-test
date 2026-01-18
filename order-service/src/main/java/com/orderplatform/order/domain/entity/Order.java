package com.orderplatform.order.domain.entity;

import com.orderplatform.order.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "shipping_id")
    private String shippingId;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expected_delivery")
    private LocalDateTime expectedDelivery;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Logic Methods
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return this.status.canTransitionTo(nextStatus);
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
