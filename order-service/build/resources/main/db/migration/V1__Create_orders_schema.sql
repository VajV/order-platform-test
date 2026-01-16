-- Удаляем V2, если он был применён с ошибками
-- (Flyway будет игнорировать этот файл, если таблицы уже существуют)

DO $$
BEGIN
    -- Создаём таблицы только если их нет
    IF NOT EXISTS (SELECT FROM pg_tables WHERE tablename = 'orders') THEN
        CREATE TABLE orders (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT NOT NULL,
            status VARCHAR(50) NOT NULL,
            total_price NUMERIC(19, 2) NOT NULL,
            payment_id VARCHAR(255),
            shipping_id VARCHAR(255),
            cancellation_reason VARCHAR(500),
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            expected_delivery TIMESTAMP
        );
    END IF;

    IF NOT EXISTS (SELECT FROM pg_tables WHERE tablename = 'order_items') THEN
        CREATE TABLE order_items (
            id BIGSERIAL PRIMARY KEY,
            order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
            product_id VARCHAR(255) NOT NULL,
            product_name VARCHAR(255) NOT NULL,
            quantity INTEGER NOT NULL,
            unit_price NUMERIC(19, 2) NOT NULL,
            total_price NUMERIC(19, 2) NOT NULL
        );
    END IF;

    IF NOT EXISTS (SELECT FROM pg_tables WHERE tablename = 'order_timeouts') THEN
        CREATE TABLE order_timeouts (
            order_id BIGINT PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
            expires_at TIMESTAMP NOT NULL,
            expected_event VARCHAR(100) NOT NULL
        );
    END IF;

    IF NOT EXISTS (SELECT FROM pg_tables WHERE tablename = 'processed_events') THEN
        CREATE TABLE processed_events (
            event_id VARCHAR(255) PRIMARY KEY,
            processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;

-- Индексы
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_timeouts_expires_at ON order_timeouts(expires_at);
CREATE INDEX IF NOT EXISTS idx_processed_events_event_id ON processed_events(event_id);
