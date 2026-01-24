# 🚀 Быстрый старт

Запуск платформы за 5 минут.

## Требования

- **Docker Desktop** (с WSL2 на Windows)
- **Java 17+** (для локальной сборки)

## Запуск

```bash
# 1. Клонировать и перейти в директорию
git clone <repo-url>
cd order-processing-platform

# 2. Скопировать переменные окружения
cp .env.example .env

# 3. Запустить всё
docker-compose up -d

# 4. Проверить статус (все должны быть healthy)
docker-compose ps
```

## Проверка работоспособности

### 1. Регистрация пользователя
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@test.com","password":"Demo123!","fullName":"Demo User"}'
```

### 2. Логин (получить JWT токен)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"Demo123!"}'
```

### 3. Создать заказ (подставить токен из п.2)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"p1","productName":"Laptop","quantity":1,"unitPrice":999.99}]}'
```

### 4. Посмотреть заказ
```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Демо полного lifecycle (требует ADMIN)
```bash
# Сначала создайте admin пользователя и получите его токен
# Затем запустите demo-lifecycle для заказа:
curl -X POST http://localhost:8080/api/orders/1/demo-lifecycle \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

Или используйте готовый скрипт:
```powershell
.\scripts\test-lifecycle.ps1
```

## Полезные URL

| Сервис | URL |
|--------|-----|
| API Gateway | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8090 |

## Остановка

```bash
docker-compose down        # Остановить
docker-compose down -v     # Остановить + удалить данные
```

## Пересборка после изменений

```bash
docker-compose up -d --build <service-name>
# Например: docker-compose up -d --build order-service
```

## Логи

```bash
docker logs -f order-service    # Логи конкретного сервиса
docker-compose logs -f          # Все логи
```
