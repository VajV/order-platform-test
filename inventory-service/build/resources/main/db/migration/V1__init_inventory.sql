-- src/main/resources/db/migration/V1__init_inventory.sql
-- Flyway migration for inventory service schema

CREATE TABLE IF NOT EXISTS inventory (
                                         id BIGSERIAL PRIMARY KEY,
                                         product_id VARCHAR(255) NOT NULL UNIQUE,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT quantity_check CHECK (total_quantity >= 0),
    CONSTRAINT reserved_check CHECK (reserved_quantity >= 0),
    CONSTRAINT reserved_not_exceed CHECK (reserved_quantity <= total_quantity)
    );

CREATE INDEX idx_product_id ON inventory(product_id);
CREATE INDEX idx_updated_at ON inventory(updated_at);

-- Track reservations for audit and compensation
CREATE TABLE IF NOT EXISTS inventory_reservation (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    order_id VARCHAR(255) NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    released_at TIMESTAMP,
    failure_reason VARCHAR(500),
    UNIQUE(order_id)
    );

CREATE INDEX idx_order_id ON inventory_reservation(order_id);
CREATE INDEX idx_inventory_status ON inventory_reservation(inventory_id, status);
CREATE INDEX idx_created_at ON inventory_reservation(created_at);

-- Function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for inventory table
CREATE TRIGGER update_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert test data
INSERT INTO inventory (product_id, total_quantity, reserved_quantity) VALUES
                                                                          ('PROD-001', 100, 0),
                                                                          ('PROD-002', 50, 5),
                                                                          ('PROD-003', 200, 10),
                                                                          ('PROD-004', 0, 0),
                                                                          ('PROD-005', 150, 20)
    ON CONFLICT (product_id) DO NOTHING;
