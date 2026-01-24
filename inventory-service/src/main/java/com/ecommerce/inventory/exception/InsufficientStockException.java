// InsufficientStockException.java
package com.ecommerce.inventory.exception;

/**
 * Exception thrown when stock is insufficient for reservation.
 */
public class InsufficientStockException extends InventoryException {

    public InsufficientStockException(String productId, Integer requested, Integer available) {
        super(
                String.format(
                        "Insufficient stock for product %s. Requested: %d, Available: %d",
                        productId, requested, available
                ),
                "INSUFFICIENT_STOCK"
        );
    }
}
