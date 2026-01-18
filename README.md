# Order Processing Platform

Event-driven платформа обработки заказов для e-commerce на Spring Boot 3 и Java 21.

## Микросервисы

- `api-gateway` — единая точка входа, rate limiting, circuit breaker.
- `auth-service` — JWT аутентификация и авторизация.
- `user-service` — управление пользователями.
- `product-service` — управление каталогом продуктов.
- `order-service` — обработка заказов.
- `inventory-service` — управление складскими остатками.
- `notification-service` — уведомления и логирование событий.

## Технологический стек

- Java 21, Spring Boot 3
- Apache Kafka
- PostgreSQL (отдельная БД на сервис)
- MongoDB (логи/аналитика)
- Redis (кэш)
- Docker + Docker Compose
- Vault (секреты)

## Переменные окружения

Создайте файл `.env` в корне проекта и заполните переменные:

```
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
POSTGRES_DB=postgres

AUTH_DB_NAME=auth_db
USER_DB_NAME=user_db
PRODUCT_DB_NAME=product_db
ORDER_DB_NAME=order_db
INVENTORY_DB_NAME=inventory_db

MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=mongo123
MONGO_INITDB_DATABASE=notifications

JWT_SECRET=ChangeMeToASecure32CharacterKey123456
VAULT_URI=http://vault:8200
VAULT_TOKEN=dev-root-token
```

## Vault (опционально)

По умолчанию конфигурация читает секреты из Vault (если доступен):

- `secret/api-gateway`
- `secret/auth-service`
- `secret/user-service`
- `secret/product-service`
- `secret/order-service`
- `secret/inventory-service`
- `secret/notification-service`

Пример:

```
vault kv put secret/auth-service jwt.secret="your-strong-secret"
```

## Запуск через Docker Compose

```
docker compose up -d
```

## Запуск микросервисов локально

```
./gradlew :api-gateway:bootRun
./gradlew :auth-service:bootRun
./gradlew :user-service:bootRun
./gradlew :product-service:bootRun
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :notification-service:bootRun
```

