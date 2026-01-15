-- V2__Fix_inventory_schema.sql

-- Таблица текущих запасов товаров
CREATE TABLE IF NOT EXISTS inventory (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE,
    total_quantity INT NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    available_quantity INT GENERATED ALWAYS AS (total_quantity - reserved_quantity) STORED,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
    );

-- Таблица резервирований товаров для заказов
CREATE TABLE IF NOT EXISTS inventory_reservations (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED' CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE CASCADE
    );

-- Индексы для быстрого поиска
CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_available ON inventory(available_quantity);
CREATE INDEX idx_reservations_inventory_id ON inventory_reservations(inventory_id);
CREATE INDEX idx_reservations_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_reservations_status ON inventory_reservations(status);
CREATE INDEX idx_reservations_expires_at ON inventory_reservations(expires_at);

-- Комментарии
COMMENT ON TABLE inventory IS 'Текущие запасы товаров (total - reserved = available)';
COMMENT ON TABLE inventory_reservations IS 'История резервирований товаров для заказов с TTL';
COMMENT ON COLUMN inventory.available_quantity IS 'Вычисляемое поле: total_quantity - reserved_quantity';
