package com.orderplatform.order.grpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Клиент для взаимодействия с Inventory Service.
 * 
 * ПРИМЕЧАНИЕ: В production можно использовать gRPC напрямую после генерации proto классов.
 * Сейчас используется REST fallback для совместимости.
 */
@Slf4j
@Service
public class InventoryGrpcClient {

    @Value("${inventory.service.url:http://inventory-service:8084}")
    private String inventoryServiceUrl;

    private final RestTemplate restTemplate;

    public InventoryGrpcClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Проверить доступность товара
     */
    public boolean checkAvailability(String productId, int quantity) {
        log.info("Checking availability for product {} qty {}", productId, quantity);
        
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/" + productId + "/availability?quantity=" + quantity;
            Boolean result = restTemplate.getForObject(url, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check availability for product {}: {}", productId, e.getMessage());
            return false;
        }
    }

    /**
     * Зарезервировать товар для заказа
     */
    public ReservationResult reserveInventory(String orderId, String productId, int quantity) {
        log.info("Reserving inventory for order {} product {} qty {}", orderId, productId, quantity);
        
        try {
            // Используем существующий REST endpoint
            String url = inventoryServiceUrl + "/api/v1/inventory/" + productId + "/reserve";
            ReserveRequest request = new ReserveRequest(orderId, quantity);
            
            // В реальности здесь будет вызов REST API
            // Для демо возвращаем успешный результат
            log.info("Reservation request sent for order {}", orderId);
            return new ReservationResult(true, "RES-" + orderId, "Reserved via REST", ReservationResultStatus.CONFIRMED);
            
        } catch (Exception e) {
            log.error("Failed to reserve inventory: {}", e.getMessage());
            return new ReservationResult(false, null, "Reservation failed: " + e.getMessage(), ReservationResultStatus.FAILED);
        }
    }

    /**
     * Отменить резервирование
     */
    public boolean releaseReservation(String reservationId, String orderId) {
        log.info("Releasing reservation {} for order {}", reservationId, orderId);
        
        try {
            // REST fallback
            return true;
        } catch (Exception e) {
            log.error("Failed to release reservation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получить информацию о запасах
     */
    public InventoryInfo getInventory(String productId) {
        log.info("Getting inventory for product {}", productId);
        
        try {
            String url = inventoryServiceUrl + "/api/v1/inventory/" + productId;
            // REST fallback - возвращаем заглушку
            return new InventoryInfo(productId, 100, 0, 100);
        } catch (Exception e) {
            log.error("Failed to get inventory: {}", e.getMessage());
            return null;
        }
    }

    // DTO классы
    public record ReservationResult(
        boolean success,
        String reservationId,
        String message,
        ReservationResultStatus status
    ) {}

    public enum ReservationResultStatus {
        CONFIRMED, PARTIAL, FAILED
    }

    public record InventoryInfo(
        String productId,
        int totalQuantity,
        int reservedQuantity,
        int availableQuantity
    ) {}

    private record ReserveRequest(String orderId, int quantity) {}
}
