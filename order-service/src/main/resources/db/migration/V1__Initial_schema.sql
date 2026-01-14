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

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id VARCHAR(255) NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             quantity INTEGER NOT NULL,
                             unit_price NUMERIC(19, 2) NOT NULL,
                             total_price NUMERIC(19, 2) NOT NULL
);

CREATE TABLE order_timeouts (
                                order_id BIGINT PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
                                expires_at TIMESTAMP NOT NULL,
                                expected_event VARCHAR(100) NOT NULL
);

CREATE TABLE processed_events (
                                  event_id VARCHAR(255) PRIMARY KEY,
                                  processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_timeouts_expires_at ON order_timeouts(expires_at);
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
