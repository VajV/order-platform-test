package com.ecommerce.inventory.grpc;

import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * gRPC сервис для управления инвентарём.
 * Предоставляет синхронный API для проверки доступности и резервирования товаров.
 * 
 * ПРИМЕЧАНИЕ: gRPC классы генерируются из inventory.proto при сборке.
 * Если IDE показывает ошибки - запустите ./gradlew build
 * 
 * Этот класс наследуется от сгенерированного InventoryServiceGrpc.InventoryServiceImplBase
 * и регистрируется как gRPC сервис через @GrpcService аннотацию.
 */
@Slf4j
@GrpcService
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    public InventoryGrpcServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, 
                                   StreamObserver<CheckAvailabilityResponse> responseObserver) {
        log.info("gRPC: Checking availability for product {} qty {}", 
                request.getProductId(), request.getQuantity());
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(request.getProductId());
        
        CheckAvailabilityResponse.Builder responseBuilder = CheckAvailabilityResponse.newBuilder();
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            boolean isAvailable = available >= request.getQuantity();
            
            responseBuilder
                    .setAvailable(isAvailable)
                    .setAvailableQuantity((int) available)
                    .setMessage(isAvailable ? "Product is available" : "Insufficient stock");
        } else {
            responseBuilder
                    .setAvailable(false)
                    .setAvailableQuantity(0)
                    .setMessage("Product not found in inventory");
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void reserveInventory(ReserveInventoryRequest request,
                                  StreamObserver<ReserveInventoryResponse> responseObserver) {
        log.info("gRPC: Reserving inventory for order {} product {} qty {}", 
                request.getOrderId(), request.getProductId(), request.getQuantity());
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdForUpdate(request.getProductId());
        
        ReserveInventoryResponse.Builder responseBuilder = ReserveInventoryResponse.newBuilder();
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            
            if (available >= request.getQuantity()) {
                inventory.setQuantityReserved(inventory.getQuantityReserved() + request.getQuantity());
                inventoryRepository.save(inventory);
                
                String reservationId = UUID.randomUUID().toString();
                log.info("gRPC: Reservation confirmed: {} for order {}", reservationId, request.getOrderId());
                
                responseBuilder
                        .setSuccess(true)
                        .setReservationId(reservationId)
                        .setMessage("Inventory reserved successfully")
                        .setStatus(com.ecommerce.inventory.grpc.ReservationStatus.RESERVATION_STATUS_CONFIRMED);
            } else {
                responseBuilder
                        .setSuccess(false)
                        .setMessage("Insufficient inventory. Available: " + available + ", Requested: " + request.getQuantity())
                        .setStatus(com.ecommerce.inventory.grpc.ReservationStatus.RESERVATION_STATUS_FAILED);
            }
        } else {
            responseBuilder
                    .setSuccess(false)
                    .setMessage("Product not found in inventory")
                    .setStatus(com.ecommerce.inventory.grpc.ReservationStatus.RESERVATION_STATUS_FAILED);
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void releaseReservation(ReleaseReservationRequest request,
                                    StreamObserver<ReleaseReservationResponse> responseObserver) {
        log.info("gRPC: Releasing reservation {} for order {}", 
                request.getReservationId(), request.getOrderId());
        
        // Note: In a real implementation, we would track reservations by ID
        // For now, we just log the release request
        ReleaseReservationResponse response = ReleaseReservationResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Reservation release request received")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getInventory(GetInventoryRequest request,
                              StreamObserver<GetInventoryResponse> responseObserver) {
        log.info("gRPC: Getting inventory for product {}", request.getProductId());
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(request.getProductId());
        
        GetInventoryResponse.Builder responseBuilder = GetInventoryResponse.newBuilder()
                .setProductId(request.getProductId());
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            
            responseBuilder
                    .setTotalQuantity(inventory.getQuantityAvailable().intValue())
                    .setReservedQuantity(inventory.getQuantityReserved().intValue())
                    .setAvailableQuantity((int) available);
        } else {
            responseBuilder
                    .setTotalQuantity(0)
                    .setReservedQuantity(0)
                    .setAvailableQuantity(0);
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // ========== Legacy methods for backward compatibility ==========

    /**
     * Проверить доступность товара
     */
    public boolean checkAvailability(String productId, int quantity) {
        log.info("Checking availability for product {} qty {}", productId, quantity);
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            return available >= quantity;
        }
        
        return false;
    }

    /**
     * Зарезервировать товар для заказа
     */
    @Transactional
    public ReservationResult reserveInventory(String orderId, String productId, int quantity) {
        log.info("Reserving inventory for order {} product {} qty {}", orderId, productId, quantity);
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdForUpdate(productId);
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            
            if (available >= quantity) {
                // Успешное резервирование
                inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
                inventoryRepository.save(inventory);
                
                String reservationId = UUID.randomUUID().toString();
                log.info("Reservation confirmed: {} for order {}", reservationId, orderId);
                
                return new ReservationResult(true, reservationId, "Inventory reserved successfully", LegacyReservationStatus.CONFIRMED);
            } else {
                return new ReservationResult(false, null, 
                    "Insufficient inventory. Available: " + available + ", Requested: " + quantity,
                    LegacyReservationStatus.FAILED);
            }
        }
        
        return new ReservationResult(false, null, "Product not found in inventory", LegacyReservationStatus.FAILED);
    }

    /**
     * Отменить резервирование (компенсация)
     */
    @Transactional
    public boolean releaseReservation(String reservationId, String orderId, String productId, int quantity) {
        log.info("Releasing reservation {} for order {}", reservationId, orderId);
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdForUpdate(productId);
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.release((long) quantity);
            inventoryRepository.save(inventory);
            return true;
        }
        
        return false;
    }

    /**
     * Получить информацию о запасах
     */
    public InventoryInfo getInventory(String productId) {
        log.info("Getting inventory for product {}", productId);
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            long available = inventory.getQuantityAvailable() - inventory.getQuantityReserved();
            
            return new InventoryInfo(
                productId,
                inventory.getQuantityAvailable().intValue(),
                inventory.getQuantityReserved().intValue(),
                (int) available
            );
        }
        
        return new InventoryInfo(productId, 0, 0, 0);
    }

    // DTO для результата резервирования
    public record ReservationResult(
        boolean success,
        String reservationId,
        String message,
        LegacyReservationStatus status
    ) {}

    public enum LegacyReservationStatus {
        CONFIRMED, PARTIAL, FAILED
    }

    // DTO для информации о запасах
    public record InventoryInfo(
        String productId,
        int totalQuantity,
        int reservedQuantity,
        int availableQuantity
    ) {}
}
