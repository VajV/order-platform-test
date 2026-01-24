-- V4__Migrate_ProductId_To_String.sql
-- Migrate inventory.product_id and inventory_reservation.product_id from BIGINT to VARCHAR(24)
-- to support MongoDB ObjectId (String) as the universal productId.

-- inventory table
ALTER TABLE inventory
    ALTER COLUMN product_id TYPE VARCHAR(24)
    USING product_id::varchar;

-- inventory_reservation table
ALTER TABLE inventory_reservation
    ALTER COLUMN product_id TYPE VARCHAR(24)
    USING product_id::varchar;

-- Recreate indexes to ensure they match the new column type
DROP INDEX IF EXISTS idx_inventory_product_id;
CREATE INDEX idx_inventory_product_id ON inventory(product_id);

DROP INDEX IF EXISTS idx_inventory_reservation_product_id;
CREATE INDEX idx_inventory_reservation_product_id ON inventory_reservation(product_id);
