package com.orderplatform.order.controller;

import com.orderplatform.order.domain.enums.OrderStatus;
import com.orderplatform.order.dto.request.OrderRequest;
import com.orderplatform.order.dto.response.OrderResponse;
import com.orderplatform.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management endpoints")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        request.setUserId(Long.valueOf(userId));

        log.info("Creating order for user: {}", request.getUserId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.info("Fetching order: {}", id);
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Get current user's orders with pagination")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(Pageable pageable) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching orders for current user: {}", userId);
        Page<OrderResponse> orders = orderService.getOrdersByUser(Long.valueOf(userId), pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get user's orders with pagination")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @RequestParam Long userId,
            Pageable pageable
    ) {
        log.info("Fetching orders for user: {}", userId);
        Page<OrderResponse> orders = orderService.getOrdersByUser(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String reason
    ) {
        log.info("Updating order {} status to: {}", id, status);
        OrderResponse response = orderService.updateOrderStatus(id, status, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel order")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "User request") String reason
    ) {
        log.info("Cancelling order: {}", id);
        orderService.cancelOrder(id, reason);
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint: Simulate full order lifecycle (NEW → RESERVED → PAID)
     * For testing/demo purposes only. Requires ADMIN role.
     */
    @PostMapping("/{id}/demo-lifecycle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Demo: Simulate full order lifecycle", 
               description = "Transitions order through NEW → RESERVED → PAID for demo purposes")
    public ResponseEntity<OrderResponse> demoLifecycle(@PathVariable Long id) {
        log.info("[DEMO] Simulating full lifecycle for order: {}", id);
        OrderResponse response = orderService.simulateFullLifecycle(id);
        return ResponseEntity.ok(response);
    }
}
