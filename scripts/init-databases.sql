-- ========================================
-- ИНИЦИАЛИЗАЦИЯ БАЗ ДАННЫХ ДЛЯ СЕРВИСОВ
-- ========================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db') THEN
        CREATE DATABASE auth_db;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db') THEN
        CREATE DATABASE user_db;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'product_db') THEN
        CREATE DATABASE product_db;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db') THEN
        CREATE DATABASE order_db;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db') THEN
        CREATE DATABASE inventory_db;
    END IF;
END $$;

GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE product_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
