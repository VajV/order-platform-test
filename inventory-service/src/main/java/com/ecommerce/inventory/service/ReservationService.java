package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.ReservationRequest;
import com.ecommerce.inventory.dto.ReservationResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryReservation;
import com.ecommerce.inventory.exception.InventoryException;
import com.ecommerce.inventory.kafka.InventoryProducer;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryProducer kafkaProducer;

    @Transactional
    public ReservationResponse reserveStock(ReservationRequest request) {
        String orderId = request.getOrderId();  // ✅ Уже String
        String productId = request.getProductId();  // ✅ Уже String
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

            // Get locked inventory
            Inventory inventory = inventoryService.getInventoryLockedForUpdate(productId);

            // Validate sufficient stock
            if (!inventory.hasAvailableQuantity(quantity)) {
                Integer available = inventory.getAvailableQuantity();
                log.warn("Insufficient stock - Product: {}, Requested: {}, Available: {}",
                        productId, quantity, available);

                InventoryReservation failedReservation = InventoryReservation.builder()
                        .inventoryId(inventory.getId())
                        .orderId(orderId)
                        .productId(inventory.getProductId())
                        .quantity(quantity)
                        .status(InventoryReservation.ReservationStatus.RELEASED)
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .failureReason(String.format(
                                "Insufficient stock. Requested: %d, Available: %d",
                                quantity, available))
                        .build();
                reservationRepository.save(failedReservation);

                kafkaProducer.sendReservationFailed(orderId, productId, quantity,
                        "Insufficient stock. Available: " + available);

                return ReservationResponse.builder()
                        .orderId(orderId)
                        .productId(productId)
                        .reservedQuantity(quantity)
                        .status(InventoryReservation.ReservationStatus.RELEASED)
                        .success(false)
                        .message(String.format(
                                "Insufficient stock for product %s. Requested: %d, Available: %d",
                                productId, quantity, available))
                        .build();
            }

            InventoryReservation reservation = InventoryReservation.builder()
                    .inventoryId(inventory.getId())
                    .orderId(orderId)
                    .productId(inventory.getProductId())
                    .quantity(quantity)
                    .status(InventoryReservation.ReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            InventoryReservation saved = reservationRepository.save(reservation);

            // Update inventory
            inventory.reserve(quantity);
            inventoryRepository.save(inventory);

            log.info("Stock reserved successfully - Order: {}, Product: {}, Quantity: {}, " +
                            "Remaining available: {}", orderId, productId, quantity,
                    inventory.getAvailableQuantity());

            kafkaProducer.sendReservationSuccess(orderId, productId, quantity, saved.getId());

            return ReservationResponse.builder()
                    .reservationId(saved.getId())  // ✅ String ID
                    .orderId(orderId)
                    .productId(productId)
                    .reservedQuantity(quantity)
                    .status(InventoryReservation.ReservationStatus.RESERVED)
                    .success(true)
                    .message("Stock reserved successfully")
                    .createdAt(saved.getCreatedAt())
                    .build();

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict during reservation, retrying...", e);
            throw new InventoryException(
                    "Conflict during reservation, please retry",
                    "RESERVATION_CONFLICT"
            );
        }
    }

    @Transactional
    public void releaseReservation(String orderId) {
        log.info("Releasing reservation for order: {}", orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)  // ✅ String
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getStatus() == InventoryReservation.ReservationStatus.RELEASED) {
            log.warn("Reservation already released for order: {}", orderId);
            return;
        }

        Inventory inventory = inventoryRepository.findById(reservation.getInventoryId())  // ✅ String
                .orElseThrow(() -> new InventoryException(
                        "Inventory not found for release",
                        "INVENTORY_NOT_FOUND"
                ));

        inventory.release(reservation.getQuantity());
        inventoryRepository.save(inventory);

        reservation.setStatus(InventoryReservation.ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation released for order: {}", orderId);

        kafkaProducer.sendReservationCompensated(
                orderId,
                inventory.getProductId(),
                reservation.getQuantity(),
                reservation.getId(),
                "Order cancelled or saga rollback"
        );
    }

    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)  // ✅ String
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getStatus() != InventoryReservation.ReservationStatus.RESERVED) {
            log.warn("Cannot confirm reservation not in RESERVED status - Order: {}, Status: {}",
                    orderId, reservation.getStatus());
            return;
        }

        reservation.setStatus(InventoryReservation.ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation confirmed for order: {}", orderId);
    }
}
