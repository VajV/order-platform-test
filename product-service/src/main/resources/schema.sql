-- Создать таблицу товаров
CREATE TABLE IF NOT EXISTS products (
                                        id BIGSERIAL PRIMARY KEY,
                                        name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Индексы
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_rating ON products(rating DESC);

-- Trigger для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_products_updated_at ON products;
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Вставить тестовые данные
INSERT INTO products (name, description, price, stock, category, image_url, active, rating) VALUES
                                                                                                ('Laptop Pro', 'High-performance laptop', 1299.99, 10, 'Electronics', 'https://example.com/laptop.jpg', TRUE, 4.5),
                                                                                                ('Wireless Mouse', 'Ergonomic wireless mouse', 29.99, 50, 'Electronics', 'https://example.com/mouse.jpg', TRUE, 4.2),
                                                                                                ('USB-C Cable', '2m USB-C charging cable', 12.99, 100, 'Accessories', 'https://example.com/cable.jpg', TRUE, 4.0),
                                                                                                ('Monitor 27"', '4K UHD Monitor', 399.99, 15, 'Electronics', 'https://example.com/monitor.jpg', TRUE, 4.7),
                                                                                                ('Mechanical Keyboard', 'RGB Mechanical Keyboard', 149.99, 25, 'Electronics', 'https://example.com/keyboard.jpg', TRUE, 4.6);
