package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.ReservationRequest;
import com.ecommerce.inventory.dto.ReservationResponse;
import com.ecommerce.inventory.service.InventoryService;
import com.ecommerce.inventory.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")  // ← ИЗМЕНЕНО: добавил /api/v1
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Management", description = "APIs for managing product inventory and stock reservations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ReservationService reservationService;

    /**
     * Create new product inventory.
     */
    @PostMapping
    @Operation(summary = "Create new inventory", description = "Initialize stock for a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inventory created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Inventory already exists for product")
    })
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody InventoryRequest request) {
        log.info("Creating inventory for product: {}", request.getProductId());
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get inventory for a product.
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product", description = "Retrieve current stock level for a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable String productId) {
        log.info("Fetching inventory for product: {}", productId);
        InventoryResponse response = inventoryService.getInventory(productId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reserve stock for an order.
     */
    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock", description = "Reserve stock for an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation processed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    public ResponseEntity<ReservationResponse> reserveStock(
            @Valid @RequestBody ReservationRequest request) {
        log.info("Processing reservation - Order: {}, Product: {}, Quantity: {}",
                request.getOrderId(), request.getProductId(), request.getQuantity());
        ReservationResponse response = reservationService.reserveStock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Release reserved stock (for failed/cancelled orders).
     */
    @PostMapping("/release/{orderId}")
    @Operation(summary = "Release reservation", description = "Release reserved stock for a cancelled order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation released"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Void> releaseReservation(
            @PathVariable String orderId) {
        log.info("Releasing reservation for order: {}", orderId);
        reservationService.releaseReservation(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Confirm reservation (after payment).
     */
    @PostMapping("/confirm/{orderId}")
    @Operation(summary = "Confirm reservation", description = "Confirm reservation after successful payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
            @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<Void> confirmReservation(
            @PathVariable String orderId) {
        log.info("Confirming reservation for order: {}", orderId);
        reservationService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Service health", description = "Check service availability")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory service is running");
    }
}
