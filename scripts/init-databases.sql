-- ========================================
-- ИНИЦИАЛИЗАЦИЯ БД ecommerce
-- Минимальная схема для ddl-auto: update
-- ========================================

\c ecommerce;

-- ========================================
-- БАЗОВЫЕ СПРАВОЧНЫЕ ТАБЛИЦЫ
-- (эти НЕ управляются JPA entities)
-- ========================================

-- Роли (для user-service)
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO roles (name, description) VALUES
                                          ('ROLE_USER', 'Default user role'),
                                          ('ROLE_MANAGER', 'Manager role with extended permissions'),
                                          ('ROLE_ADMIN', 'Administrator role with full access')
    ON CONFLICT (name) DO NOTHING;

-- Категории (для product-service)
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO categories (name, description) VALUES
                                               ('Electronics', 'Electronic devices and gadgets'),
                                               ('Accessories', 'Computer and phone accessories'),
                                               ('Office', 'Office supplies and equipment'),
                                               ('Software', 'Software and licenses')
    ON CONFLICT (name) DO NOTHING;

-- ========================================
-- ВАЖНО: Все остальные таблицы
-- (users, products, orders, inventory, etc.)
-- будут созданы АВТОМАТИЧЕСКИ через
-- ddl-auto: update в каждом микросервисе!
-- ========================================

-- Показать статистику
SELECT
    'Database initialization completed!' AS status,
    (SELECT COUNT(*) FROM roles) AS total_roles,
    (SELECT COUNT(*) FROM categories) AS total_categories;
