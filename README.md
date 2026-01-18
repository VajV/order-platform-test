# Order Processing Platform

Event-driven платформа обработки заказов для e-commerce на **Spring Boot 3.2** и **Java 21**.

## 🏗️ Архитектура

### Микросервисы

| Сервис | Порт | Описание | База данных |
|--------|------|----------|-------------|
| `api-gateway` | 8080 | Единая точка входа, rate limiting, JWT validation | Redis |
| `auth-service` | 8087 | JWT аутентификация и авторизация | PostgreSQL |
| `user-service` | 8081 | Управление пользователями и профилями | PostgreSQL |
| `product-service` | 8084 | Каталог продуктов | PostgreSQL |
| `order-service` | 8083 | Обработка заказов (Event-Driven Saga) | PostgreSQL |
| `inventory-service` | 8085 | Управление складскими остатками | PostgreSQL + Redis |
| `notification-service` | 8086 | Email/SMS уведомления | MongoDB |

### Инфраструктура

- **PostgreSQL 15** — основная БД (отдельная база на сервис)
- **MongoDB 7** — документы, логи, шаблоны уведомлений
- **Redis 7** — кэш, rate limiting, distributed locks
- **Apache Kafka 7.5** — Event Streaming
- **Schema Registry** — управление схемами Kafka
- **HashiCorp Vault** — управление секретами (опционально)

## 🚀 Быстрый старт

### Предварительные требования

- Java 21
- Docker & Docker Compose
- Gradle 8+ (или используйте `./gradlew`)

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd order-processing-platform
```

### 2. Конфигурация секретов

**ВАЖНО:** Создайте файл `.env` на основе шаблона:

```bash
cp .env.example .env
```

Отредактируйте `.env` и **обязательно замените** все значения `CHANGE_ME`:

```bash
# Минимальные изменения для запуска:
POSTGRES_PASSWORD=your_strong_postgres_password_here
MONGO_INITDB_ROOT_PASSWORD=your_strong_mongo_password_here

# КРИТИЧЕСКИ ВАЖНО - JWT Secret (минимум 32 символа)
# Генерация: openssl rand -base64 32
JWT_SECRET=$(openssl rand -base64 32)
```

> **⚠️ SECURITY WARNING:**
> - НЕ используйте дефолтные пароли в production!
> - НЕ коммитьте `.env` файл в Git!
> - Используйте Vault или Kubernetes Secrets в production!

### 3. Запуск через Docker Compose

> **✅ Базы данных создаются автоматически!**  
> При первом запуске PostgreSQL автоматически создаст: `auth_db`, `user_db`, `product_db`, `order_db`, `inventory_db`

#### Вариант A: Полная система (инфраструктура + сервисы)

```bash
docker compose up -d
```

#### Вариант B: Только инфраструктура (для локальной разработки)

```bash
docker compose -f docker-compose-infra.yml up -d
```

Затем запускайте сервисы локально:

```bash
./gradlew :auth-service:bootRun
./gradlew :api-gateway:bootRun
# и т.д.
```

### 4. Проверка работоспособности

```bash
# Проверка здоровья всех сервисов
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8087/actuator/health  # Auth Service
curl http://localhost:8081/actuator/health  # User Service
# ...

# Kafka UI
open http://localhost:8090

# Schema Registry
curl http://localhost:8082/subjects
```

## 🛠️ Разработка

### Сборка проекта

```bash
# Полная сборка всех модулей
./gradlew clean build

# Сборка конкретного сервиса
./gradlew :auth-service:build

# Пропустить тесты
./gradlew build -x test
```

### Запуск тестов

```bash
# Все тесты
./gradlew test

# Тесты конкретного модуля
./gradlew :user-service:test

# Интеграционные тесты (требуют Testcontainers)
./gradlew :auth-service:integrationTest
```

### Проверка кода

```bash
# Checkstyle
./gradlew checkstyleMain

# SpotBugs
./gradlew spotbugsMain
```

## 📦 Docker образы

### Пересборка образов

```bash
# Все сервисы
docker compose build --no-cache

# Конкретный сервис
docker compose build --no-cache auth-service
```

### Просмотр логов

```bash
# Все сервисы
docker compose logs -f

# Конкретный сервис
docker compose logs -f auth-service

# Последние 100 строк
docker compose logs --tail=100 order-service
```

## 🔐 Безопасность

### JWT Токены

- **Access Token**: 1 час (по умолчанию)
- **Refresh Token**: 7 дней (по умолчанию)
- Алгоритм: HS256 (можно переключить на RS256)

### Получение токена

```bash
# Регистрация
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Логин
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!"
  }'

# Использование токена
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### Vault (опционально)

Если используете Vault:

```bash
# Запуск Vault в dev режиме (через docker-compose уже запущен)
docker exec -it vault sh

# Сохранение секретов
vault kv put secret/auth-service jwt.secret="your-super-secret-key"
vault kv put secret/auth-service db.password="secure-db-password"

# Чтение секретов
vault kv get secret/auth-service
```

## 📊 Мониторинг

### Actuator Endpoints

Все сервисы предоставляют:

- `/actuator/health` — статус здоровья
- `/actuator/metrics` — метрики
- `/actuator/prometheus` — экспорт для Prometheus
- `/actuator/info` — информация о приложении

### Kafka Monitoring

Kafka UI доступен на: http://localhost:8090

### Database

```bash
# PostgreSQL
docker exec -it postgres psql -U postgres

# Список баз
\l

# Подключение к базе
\c auth_db

# Список таблиц
\dt

# MongoDB
docker exec -it mongodb mongosh -u admin -p <password>

# Список баз
show dbs

# Использование базы
use notifications

# Список коллекций
show collections
```

## 🧪 Тестирование API

### Postman Collection

Импортируйте коллекцию из `docs/postman/Order-Platform.postman_collection.json`

### cURL примеры

#### Auth Service

```bash
# Регистрация пользователя
curl -X POST http://localhost:8087/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test123!"}'

# Логин
curl -X POST http://localhost:8087/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}'
```

#### Product Service

```bash
# Список продуктов
curl http://localhost:8084/api/products

# Создание продукта (требует ADMIN роль)
curl -X POST http://localhost:8084/api/products \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Laptop",
    "description":"High-end laptop",
    "price":1299.99,
    "stock":50,
    "categoryId":"1"
  }'
```

## 📚 Документация

- Swagger UI доступен для каждого сервиса:
  - Auth: http://localhost:8087/swagger-ui.html
  - User: http://localhost:8081/swagger-ui.html
  - Product: http://localhost:8084/swagger-ui.html
  - Order: http://localhost:8083/swagger-ui.html
  - Inventory: http://localhost:8085/swagger-ui.html

## 🐛 Troubleshooting

### Проблема: "JWT Secret not set"

**Решение:** Убедитесь, что `JWT_SECRET` установлен в `.env` файле и имеет длину минимум 32 символа.

### Проблема: "Cannot connect to PostgreSQL"

**Решение:**
```bash
# Проверьте статус контейнера
docker ps | grep postgres

# Проверьте логи
docker logs postgres

# Пересоздайте контейнер
docker compose down -v
docker compose up -d postgres
```

### Проблема: "Flyway migration failed"

**Решение:**
```bash
# Очистите volume PostgreSQL (ВНИМАНИЕ: удалит все данные)
docker compose down -v
docker compose up -d
```

### Проблема: "Port already in use"

**Решение:**
```bash
# Найдите процесс на порту
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Убейте процесс или измените порт в docker-compose.yml
```

## 🤝 Contributing

1. Создайте feature branch (`git checkout -b feature/amazing-feature`)
2. Коммитьте изменения (`git commit -m 'Add amazing feature'`)
3. Пушьте в branch (`git push origin feature/amazing-feature`)
4. Откройте Pull Request

## 📝 License

MIT License - see [LICENSE](LICENSE) file

## 👥 Authors

- **Your Name** - [GitHub](https://github.com/yourusername)

## 🙏 Acknowledgments

- Spring Boot Team
- Apache Kafka Community
- HashiCorp Vault Team
