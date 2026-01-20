package com.orderplatform.order.grpc;

import com.ecommerce.inventory.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * gRPC клиент для взаимодействия с Inventory Service.
 * Используется для синхронной проверки доступности и резервирования товаров.
 */
@Slf4j
@Service
public class InventoryGrpcClient {

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    /**
     * Проверить доступность товара
     * 
     * @param productId ID товара
     * @param quantity требуемое количество
     * @return true если товар доступен в нужном количестве
     */
    public boolean checkAvailability(String productId, int quantity) {
        log.info("Checking availability for product {} qty {}", productId, quantity);
        
        try {
            CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
            
            CheckAvailabilityResponse response = inventoryStub.checkAvailability(request);
            
            log.info("Availability check result: available={}, qty={}", 
                    response.getAvailable(), response.getAvailableQuantity());
            
            return response.getAvailable();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus(), e);
            return false;
        }
    }

    /**
     * Зарезервировать товар для заказа
     * 
     * @param orderId ID заказа
     * @param productId ID товара
     * @param quantity количество для резервирования
     * @return результат резервирования
     */
    public ReservationResult reserveInventory(String orderId, String productId, int quantity) {
        log.info("Reserving inventory for order {} product {} qty {}", orderId, productId, quantity);
        
        try {
            ReserveInventoryRequest request = ReserveInventoryRequest.newBuilder()
                .setOrderId(orderId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
            
            ReserveInventoryResponse response = inventoryStub.reserveInventory(request);
            
            log.info("Reservation result: success={}, reservationId={}", 
                    response.getSuccess(), response.getReservationId());
            
            return new ReservationResult(
                response.getSuccess(),
                response.getReservationId(),
                response.getMessage(),
                mapStatus(response.getStatus())
            );
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC reservation failed: {}", e.getStatus(), e);
            return new ReservationResult(false, null, "gRPC call failed: " + e.getStatus(), ReservationResultStatus.FAILED);
        }
    }

    /**
     * Отменить резервирование (для компенсации в Saga)
     * 
     * @param reservationId ID резервирования
     * @param orderId ID заказа
     * @return true если отмена успешна
     */
    public boolean releaseReservation(String reservationId, String orderId) {
        log.info("Releasing reservation {} for order {}", reservationId, orderId);
        
        try {
            ReleaseReservationRequest request = ReleaseReservationRequest.newBuilder()
                .setReservationId(reservationId)
                .setOrderId(orderId)
                .build();
            
            ReleaseReservationResponse response = inventoryStub.releaseReservation(request);
            
            log.info("Release result: success={}", response.getSuccess());
            return response.getSuccess();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC release failed: {}", e.getStatus(), e);
            return false;
        }
    }

    /**
     * Получить информацию о запасах
     * 
     * @param productId ID товара
     * @return информация о запасах или null при ошибке
     */
    public InventoryInfo getInventory(String productId) {
        log.info("Getting inventory for product {}", productId);
        
        try {
            GetInventoryRequest request = GetInventoryRequest.newBuilder()
                .setProductId(productId)
                .build();
            
            GetInventoryResponse response = inventoryStub.getInventory(request);
            
            return new InventoryInfo(
                response.getProductId(),
                response.getTotalQuantity(),
                response.getReservedQuantity(),
                response.getAvailableQuantity()
            );
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC getInventory failed: {}", e.getStatus(), e);
            return null;
        }
    }

    private ReservationResultStatus mapStatus(ReservationStatus status) {
        return switch (status) {
            case RESERVATION_STATUS_CONFIRMED -> ReservationResultStatus.CONFIRMED;
            case RESERVATION_STATUS_PARTIAL -> ReservationResultStatus.PARTIAL;
            default -> ReservationResultStatus.FAILED;
        };
    }

    // DTO для результата резервирования
    public record ReservationResult(
        boolean success,
        String reservationId,
        String message,
        ReservationResultStatus status
    ) {}

    public enum ReservationResultStatus {
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

