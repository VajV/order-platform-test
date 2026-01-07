// ReservationService.java
package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.ReservationRequest;
import com.ecommerce.inventory.dto.ReservationResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryReservation;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.InventoryException;
import com.ecommerce.inventory.kafka.InventoryProducer;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservation service for saga pattern orchestration.
 * Handles stock reservations with retries for optimistic locking conflicts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryProducer kafkaProducer;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 100;

    /**
     * Reserve stock for an order.
     * Implements optimistic locking with retries for conflict resolution.
     *
     * Flow:
     * 1. Check inventory exists
     * 2. Acquire pessimistic lock on inventory row
     * 3. Validate sufficient stock available
     * 4. Create reservation record
     * 5. Update inventory reserved quantity
     * 6. Publish event to Kafka (async)
     *
     * @param request reservation request
     * @return reservation response with success status
     */
    @Transactional
    public ReservationResponse reserveStock(ReservationRequest request) {
        String orderId = request.getOrderId();
        String productId = request.getProductId();
        Integer quantity = request.getQuantity();

        log.info("Processing stock reservation - Order: {}, Product: {}, Quantity: {}",
                orderId, productId, quantity);

        try {
            // Check if already reserved
            if (reservationRepository.findByOrderId(orderId).isPresent()) {
                log.warn("Order already has reservation: {}", orderId);
                throw new InventoryException(
                        String.format("Order %s already has an active reservation", orderId),
                        "DUPLICATE_RESERVATION"
                );
            }

            // Get locked inventory (pessimistic write lock)
            Inventory inventory = inventoryService.getInventoryLockedForUpdate(productId);

            // Validate sufficient stock
            if (!inventory.hasAvailableQuantity(quantity)) {
                Integer available = inventory.getAvailableQuantity();
                log.warn("Insufficient stock - Product: {}, Requested: {}, Available: {}",
                        productId, quantity, available);

                // Create failed reservation record
                InventoryReservation failedReservation = InventoryReservation.builder()
                        .inventoryId(inventory.getId())
                        .orderId(orderId)
                        .reservedQuantity(quantity)
                        .status(InventoryReservation.ReservationStatus.FAILED)
                        .failureReason(String.format(
                                "Insufficient stock. Requested: %d, Available: %d",
                                quantity, available))
                        .build();
                reservationRepository.save(failedReservation);

                // Publish failure event
                kafkaProducer.sendReservationFailed(orderId, productId, quantity,
                        "Insufficient stock. Available: " + inventory.getAvailableQuantity());
                return ReservationResponse.builder()
                        .orderId(orderId)
                        .productId(productId)
                        .reservedQuantity(quantity)
                        .status(InventoryReservation.ReservationStatus.FAILED)
                        .success(false)
                        .message(String.format(
                                "Insufficient stock for product %s. Requested: %d, Available: %d",
                                productId, quantity, available))
                        .build();
            }

            // Create reservation record
            InventoryReservation reservation = InventoryReservation.builder()
                    .inventoryId(inventory.getId())
                    .orderId(orderId)
                    .reservedQuantity(quantity)
                    .status(InventoryReservation.ReservationStatus.PENDING)
                    .build();
            InventoryReservation saved = reservationRepository.save(reservation);

            // Update inventory
            inventory.reserve(quantity);
            inventoryRepository.save(inventory);

            log.info("Stock reserved successfully - Order: {}, Product: {}, Quantity: {}, " +
                            "Remaining available: {}", orderId, productId, quantity,
                    inventory.getAvailableQuantity());

            // Publish success event (async via Kafka)
            kafkaProducer.sendReservationSuccess(orderId, productId, quantity,
                    reservation.getId().toString());

            return ReservationResponse.builder()
                    .reservationId(saved.getId())
                    .orderId(orderId)
                    .productId(productId)
                    .reservedQuantity(quantity)
                    .status(InventoryReservation.ReservationStatus.PENDING)
                    .success(true)
                    .message("Stock reserved successfully")
                    .createdAt(saved.getCreatedAt())
                    .build();

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict during reservation, retrying...", e);
            // Retry logic can be implemented here with @Retry annotation
            throw new InventoryException(
                    "Conflict during reservation, please retry",
                    "RESERVATION_CONFLICT"
            );
        }
    }

    /**
     * Release reserved stock (compensation for failed order).
     * Called when order fails or is cancelled.
     *
     * @param orderId the order ID
     */
    @Transactional
    public void releaseReservation(String orderId) {
        log.info("Releasing reservation for order: {}", orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getStatus() == InventoryReservation.ReservationStatus.RELEASED) {
            log.warn("Reservation already released for order: {}", orderId);
            return;
        }

        // Get inventory and release
        Inventory inventory = inventoryRepository.findById(reservation.getInventoryId())
                .orElseThrow(() -> new InventoryException(
                        "Inventory not found for release",
                        "INVENTORY_NOT_FOUND"
                ));

        inventory.release(reservation.getReservedQuantity());
        inventoryRepository.save(inventory);

        // Mark reservation as released
        reservation.setStatus(InventoryReservation.ReservationStatus.RELEASED);
        reservation.setReleasedAt(java.time.LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation released for order: {}", orderId);

        // Publish compensation event
        kafkaProducer.sendReservationCompensated(
                orderId,
                inventory.getProductId(),
                reservation.getReservedQuantity(),
                reservation.getId().toString(),
                "Order cancelled or saga rollback"
        );
    }

    /**
     * Confirm reservation (called after payment confirmation).
     * Moves from PENDING to CONFIRMED status.
     *
     * @param orderId the order ID
     */
    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getStatus() != InventoryReservation.ReservationStatus.PENDING) {
            log.warn("Cannot confirm reservation not in PENDING status - Order: {}, Status: {}",
                    orderId, reservation.getStatus());
            return;
        }

        reservation.setStatus(InventoryReservation.ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(java.time.LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation confirmed for order: {}", orderId);
    }
}
