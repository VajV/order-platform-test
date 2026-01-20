-- Create Inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity_available BIGINT NOT NULL DEFAULT 0,
    quantity_reserved BIGINT NOT NULL DEFAULT 0,
    reorder_level BIGINT NOT NULL DEFAULT 10,
    warehouse_location VARCHAR(255),
    last_restocked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_quantity_available ON inventory(quantity_available);
COMMENT ON TABLE inventory IS 'Product inventory and stock levels';
COMMENT ON COLUMN inventory.quantity_available IS 'Total available quantity for sale';
COMMENT ON COLUMN inventory.quantity_reserved IS 'Quantity reserved in pending orders';
COMMENT ON COLUMN inventory.version IS 'Optimistic locking version for concurrency control';

-- Create Inventory Reservations table
CREATE TABLE IF NOT EXISTS inventory_reservation (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity_reserved BIGINT NOT NULL,
    reservation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_reservation_order_id ON inventory_reservation(order_id);
CREATE INDEX idx_inventory_reservation_product_id ON inventory_reservation(product_id);
CREATE INDEX idx_inventory_reservation_status ON inventory_reservation(reservation_status);
CREATE INDEX idx_inventory_reservation_expires_at ON inventory_reservation(expires_at);
COMMENT ON TABLE inventory_reservation IS 'Temporary inventory reservations for orders';
COMMENT ON COLUMN inventory_reservation.reservation_status IS 'Status: PENDING, CONFIRMED, RELEASED, EXPIRED';
COMMENT ON COLUMN inventory_reservation.expires_at IS 'When this reservation auto-releases if not confirmed';
