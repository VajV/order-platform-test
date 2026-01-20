package com.ecommerce.inventory.grpc;

import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * gRPC сервис для управления инвентарём.
 * Предоставляет синхронный API для проверки доступности и резервирования товаров.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepository;

    /**
     * Проверить доступность товара
     */
    @Override
    public void checkAvailability(CheckAvailabilityRequest request, 
                                   StreamObserver<CheckAvailabilityResponse> responseObserver) {
        log.info("gRPC: Checking availability for product {} qty {}", 
                request.getProductId(), request.getQuantity());
        
        try {
            Long productId = Long.parseLong(request.getProductId());
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
            
            CheckAvailabilityResponse.Builder responseBuilder = CheckAvailabilityResponse.newBuilder();
            
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
                boolean isAvailable = available >= request.getQuantity();
                
                responseBuilder
                    .setAvailable(isAvailable)
                    .setAvailableQuantity(available)
                    .setMessage(isAvailable ? "Product available" : "Insufficient stock");
            } else {
                responseBuilder
                    .setAvailable(false)
                    .setAvailableQuantity(0)
                    .setMessage("Product not found in inventory");
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (NumberFormatException e) {
            log.error("Invalid product ID format: {}", request.getProductId());
            responseObserver.onNext(CheckAvailabilityResponse.newBuilder()
                .setAvailable(false)
                .setAvailableQuantity(0)
                .setMessage("Invalid product ID format")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking availability", e);
            responseObserver.onError(e);
        }
    }

    /**
     * Зарезервировать товар для заказа
     */
    @Override
    @Transactional
    public void reserveInventory(ReserveInventoryRequest request,
                                  StreamObserver<ReserveInventoryResponse> responseObserver) {
        log.info("gRPC: Reserving inventory for order {} product {} qty {}",
                request.getOrderId(), request.getProductId(), request.getQuantity());
        
        try {
            Long productId = Long.parseLong(request.getProductId());
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdForUpdate(productId);
            
            ReserveInventoryResponse.Builder responseBuilder = ReserveInventoryResponse.newBuilder();
            
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
                
                if (available >= request.getQuantity()) {
                    // Успешное резервирование
                    inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
                    inventoryRepository.save(inventory);
                    
                    String reservationId = UUID.randomUUID().toString();
                    
                    responseBuilder
                        .setSuccess(true)
                        .setReservationId(reservationId)
                        .setStatus(ReservationStatus.RESERVATION_STATUS_CONFIRMED)
                        .setMessage("Inventory reserved successfully");
                    
                    log.info("Reservation confirmed: {} for order {}", reservationId, request.getOrderId());
                } else {
                    // Недостаточно товара
                    responseBuilder
                        .setSuccess(false)
                        .setReservationId("")
                        .setStatus(ReservationStatus.RESERVATION_STATUS_FAILED)
                        .setMessage("Insufficient inventory. Available: " + available + ", Requested: " + request.getQuantity());
                }
            } else {
                responseBuilder
                    .setSuccess(false)
                    .setReservationId("")
                    .setStatus(ReservationStatus.RESERVATION_STATUS_FAILED)
                    .setMessage("Product not found in inventory");
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (NumberFormatException e) {
            log.error("Invalid product ID format: {}", request.getProductId());
            responseObserver.onNext(ReserveInventoryResponse.newBuilder()
                .setSuccess(false)
                .setStatus(ReservationStatus.RESERVATION_STATUS_FAILED)
                .setMessage("Invalid product ID format")
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error reserving inventory", e);
            responseObserver.onError(e);
        }
    }

    /**
     * Отменить резервирование (компенсация)
     */
    @Override
    @Transactional
    public void releaseReservation(ReleaseReservationRequest request,
                                    StreamObserver<ReleaseReservationResponse> responseObserver) {
        log.info("gRPC: Releasing reservation {} for order {}", 
                request.getReservationId(), request.getOrderId());
        
        // В реальной системе здесь был бы поиск по reservationId
        // Для демонстрации просто возвращаем успех
        
        responseObserver.onNext(ReleaseReservationResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Reservation released")
            .build());
        responseObserver.onCompleted();
    }

    /**
     * Получить информацию о запасах
     */
    @Override
    public void getInventory(GetInventoryRequest request,
                              StreamObserver<GetInventoryResponse> responseObserver) {
        log.info("gRPC: Getting inventory for product {}", request.getProductId());
        
        try {
            Long productId = Long.parseLong(request.getProductId());
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
            
            GetInventoryResponse.Builder responseBuilder = GetInventoryResponse.newBuilder()
                .setProductId(request.getProductId());
            
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
                
                responseBuilder
                    .setTotalQuantity(inventory.getTotalQuantity())
                    .setReservedQuantity(inventory.getReservedQuantity())
                    .setAvailableQuantity(available);
            } else {
                responseBuilder
                    .setTotalQuantity(0)
                    .setReservedQuantity(0)
                    .setAvailableQuantity(0);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error getting inventory", e);
            responseObserver.onError(e);
        }
    }
}

