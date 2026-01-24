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
        Long orderId = Long.parseLong(request.getOrderId());
        String productId = request.getProductId();
        Integer quantity = request.getQuantity();

        log.info("Processing stock reservation - Order: {}, Product: {}, Quantity: {}",
                orderId, productId, quantity);

        try {
            if (reservationRepository.findByOrderId(orderId).isPresent()) {
                log.warn("Order already has reservation: {}", orderId);
                throw new InventoryException(
                        String.format("Order %s already has an active reservation", orderId),
                        "DUPLICATE_RESERVATION"
                );
            }

            Inventory inventory = inventoryService.getInventoryLockedForUpdate(productId);

            if (!inventory.hasAvailableQuantity(quantity.longValue())) {
                Long available = inventory.getAvailableQuantity();
                log.warn("Insufficient stock - Product: {}, Requested: {}, Available: {}",
                        productId, quantity, available);

                InventoryReservation failedReservation = InventoryReservation.builder()
                        .orderId(orderId)
                        .productId(inventory.getProductId())
                        .quantityReserved(quantity.longValue())
                        .reservationStatus(InventoryReservation.ReservationStatus.RELEASED)
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .build();
                reservationRepository.save(failedReservation);

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

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(orderId)
                    .productId(inventory.getProductId())
                    .quantityReserved(quantity.longValue())
                    .reservationStatus(InventoryReservation.ReservationStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            InventoryReservation saved = reservationRepository.save(reservation);

            inventory.reserve(quantity.longValue());
            inventoryRepository.save(inventory);

            log.info("Stock reserved successfully - Order: {}, Product: {}, Quantity: {}, " +
                            "Remaining available: {}", orderId, productId, quantity,
                    inventory.getAvailableQuantity());

            kafkaProducer.sendReservationSuccess(orderId.toString(), productId, quantity,
                    saved.getId().toString());

            return ReservationResponse.builder()
                    .reservationId(saved.getId().toString())
                    .orderId(orderId.toString())
                    .productId(productId)
                    .reservedQuantity(quantity)
                    .status(InventoryReservation.ReservationStatus.PENDING)
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

        Long id = Long.parseLong(orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(id)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getReservationStatus() == InventoryReservation.ReservationStatus.RELEASED) {
            log.warn("Reservation already released for order: {}", orderId);
            return;
        }

        Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new InventoryException(
                        "Inventory not found for release",
                        "INVENTORY_NOT_FOUND"
                ));

        inventory.release(reservation.getQuantityReserved());
        inventoryRepository.save(inventory);

        reservation.setReservationStatus(InventoryReservation.ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation released for order: {}", orderId);

        kafkaProducer.sendReservationCompensated(
                orderId,
                inventory.getProductId(),
                reservation.getQuantityReserved().intValue(),
                reservation.getId().toString(),
                "Order cancelled or saga rollback"
        );
    }

    @Transactional
    public void confirmReservation(String orderId) {
        log.info("Confirming reservation for order: {}", orderId);

        Long id = Long.parseLong(orderId);

        InventoryReservation reservation = reservationRepository.findByOrderId(id)
                .orElseThrow(() -> new InventoryException(
                        String.format("Reservation not found for order %s", orderId),
                        "RESERVATION_NOT_FOUND"
                ));

        if (reservation.getReservationStatus() != InventoryReservation.ReservationStatus.PENDING) {
            log.warn("Cannot confirm reservation not in PENDING status - Order: {}, Status: {}",
                    orderId, reservation.getReservationStatus());
            return;
        }

        reservation.setReservationStatus(InventoryReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("Reservation confirmed for order: {}", orderId);
    }
}
