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
import java.util.UUID;

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
        // ✅ ИСПРАВЛЕНО: Преобразуем String -> UUID
        UUID orderId = UUID.fromString(request.getOrderId());
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

            // Get locked inventory
            Inventory inventory = inventoryService.getInventoryLockedForUpdate(productId);

            // Validate sufficient stock
            if (!inventory.hasAvailableQuantity(quantity)) {
                Integer available = inventory.getAvailableQuantity();
                log.warn("Insufficient stock - Product: {}, Requested: {}, Available: {}",
                        productId, quantity, available);

                // ✅ ИСПРАВЛЕНО: RESERVED вместо FAILED, quantity вместо reservedQuantity
                InventoryReservation failedReservation = InventoryReservation.builder()
                        .inventoryId(inventory.getId())
                        .orderId(orderId)
                        .productId(inventory.getProductId())
                        .quantity(quantity)  // ✅ ИСПРАВЛЕНО
                        .status(InventoryReservation.ReservationStatus.RELEASED)  // ✅ ИСПРАВЛЕНО
                        .expiresAt(LocalDateTime.now().plusMinutes(15))  // ✅ ДОБАВЛЕНО
                        .failureReason(String.format(
                                "Insufficient stock. Requested: %d, Available: %d",
                                quantity, available))
                        .build();
                reservationRepository.save(failedReservation);

                // Publish failure event
                kafkaProducer.sendReservationFailed(orderId.toString(), productId, quantity,
                        "Insufficient stock. Available: " + available);

                return ReservationResponse.builder()
                        .orderId(orderId.toString())
                        .productId(productId)
                        .reservedQuantity(quantity)
                        .status(InventoryReservation.ReservationStatus.RELEASED)
                        .success(false)
                        .message(String.format(
                                "Insufficient stock for product %s. Requested: %d, Available: %d",
                                productId, quantity, available))
                        .build();
            }

            // ✅ ИСПРАВЛЕНО: RESERVED вместо PENDING
            InventoryReservation reservation = InventoryReservation.builder()
                    .inventoryId(inventory.getId())
                    .orderId(orderId)
                    .productId(inventory.getProductId())
                    .quantity(quantity)  // ✅ ИСПРАВЛЕНО
                    .status(InventoryReservation.ReservationStatus.RESERVED)  // ✅ ИСПРАВЛЕНО
                    .expiresAt(LocalDateTime.now().plusMinutes(15))  // ✅ ДОБАВЛЕНО
                    .build();
            InventoryReservation saved = reservationRepository.save(reservation);

            // Update inventory
            inventory.reserve(quantity);
            inventoryRepository.save(inventory);

            log.info("Stock reserved successfully - Order: {}, Product: {}, Quantity: {}, " +
                            "Remaining available: {}", orderId, productId, quantity,
                    inventory.getAvailableQuantity());

            // Publish success event
            kafkaProducer.sendReservationSuccess(orderId.toString(), productId, quantity,
                    saved.getId().toString());

            return ReservationResponse.builder()
                    .reservationId(saved.getId().toString())  // ✅ ИСПРАВЛЕНО: UUID -> String
                    .orderId(orderId.toString())
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

        UUID uuid = UUID.fromString(orderId);  // ✅ String -> UUID

        InventoryReservation reservation = reservationRepository.findByOrderId(uuid)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getStatus() == InventoryReservation.ReservationStatus.RELEASED) {
            log.warn("Reservation already released for order: {}", orderId);
            return;
        }

        // ✅ ИСПРАВЛЕНО: findById теперь принимает UUID
        Inventory inventory = inventoryRepository.findById(reservation.getInventoryId())
                .orElseThrow(() -> new InventoryException(
                        "Inventory not found for release",
                        "INVENTORY_NOT_FOUND"
                ));

        inventory.release(reservation.getQuantity());  // ✅ ИСПРАВЛЕНО
        inventoryRepository.save(inventory);

        reservation.setStatus(InventoryReservation.ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation released for order: {}", orderId);

        kafkaProducer.sendReservationCompensated(
                orderId,
                inventory.getProductId().toString(),
                reservation.getQuantity(),  // ✅ ИСПРАВЛЕНО
                reservation.getId().toString(),
                "Order cancelled or saga rollback"
        );
    }

    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        UUID uuid = UUID.fromString(orderId);  // ✅ String -> UUID

        InventoryReservation reservation = reservationRepository.findByOrderId(uuid)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        // ✅ ИСПРАВЛЕНО: RESERVED вместо PENDING
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
