# 🚀 Быстрый старт и проверка работоспособности

Полная инструкция по запуску и тестированию Order Processing Platform.

---

## 📋 Требования

### Обязательные:
- **Java 21+** (JDK)
- **Docker Desktop** (для Windows: WSL 2)
- **Docker Compose** (входит в Docker Desktop)
- **Git**

### Опциональные:
- **Make** (для Windows: [chocolatey](https://chocolatey.org/install) или используйте PowerShell команды)
- **k3d** (для Kubernetes локально)
- **Helm 3+** (для Kubernetes)

---

## 🔧 Шаг 1: Клонирование и настройка

### 1.1 Клонировать репозиторий

```bash
git clone https://github.com/VajV/order-platform-test.git
cd order-processing-platform
```

### 1.2 Проверить Java версию

```bash
# Windows (PowerShell)
java -version

# Должно быть: openjdk version "21" или выше
```

Если Java не установлена:
- **Windows:** Скачать [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)
- **Linux/Mac:** `sudo apt install openjdk-21-jdk` или `brew install openjdk@21`

### 1.3 Настроить переменные окружения

```bash
# Windows (PowerShell)
Copy-Item .env.example .env

# Linux/Mac
cp .env.example .env
```

**Важно:** Отредактируйте `.env` файл и установите:
- `JWT_SECRET` - сгенерируйте безопасный ключ (минимум 32 символа):
  ```bash
  # Linux/Mac
  openssl rand -base64 32
  
  # Windows (PowerShell)
  -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | % {[char]$_})
  ```

---

## 🐳 Шаг 2: Запуск инфраструктуры

### 2.1 Запустить только инфраструктуру (рекомендуется для первого запуска)

```bash
# Windows (PowerShell)
docker-compose -f docker-compose-infra.yml up -d

# Linux/Mac (или через Make)
make dev-infra
```

**Проверить статус:**
```bash
docker-compose ps
```

**Ожидаемый результат:**
```
NAME                    STATUS              PORTS
postgres                Up (healthy)        0.0.0.0:5432->5432/tcp
mongodb                 Up (healthy)        0.0.0.0:27017->27017/tcp
redis                   Up (healthy)        0.0.0.0:6379->6379/tcp
zookeeper               Up                  0.0.0.0:2181->2181/tcp
kafka                   Up (healthy)        0.0.0.0:9092->9092/tcp
schema-registry         Up                  0.0.0.0:8081->8081/tcp
kafka-ui                Up                  0.0.0.0:8088->8088/tcp
```

### 2.2 Проверить логи инфраструктуры

```bash
# Все сервисы
docker-compose logs

# Конкретный сервис
docker-compose logs postgres
docker-compose logs kafka
```

**Если есть ошибки:**
```bash
# Пересоздать контейнеры
docker-compose down -v
docker-compose -f docker-compose-infra.yml up -d
```

---

## 🏗️ Шаг 3: Сборка проекта

### 3.1 Собрать все сервисы

```bash
# Windows (PowerShell)
.\gradlew clean build -x test

# Linux/Mac
./gradlew clean build -x test
```

**Ожидаемый результат:**
```
BUILD SUCCESSFUL in 2m 30s
```

### 3.2 Проверить созданные JAR файлы

```bash
# Windows (PowerShell)
Get-ChildItem -Recurse -Filter "*.jar" -Path "*/build/libs" | Select-Object FullName

# Linux/Mac
find . -name "*.jar" -path "*/build/libs/*"
```

**Должны быть созданы:**
- `api-gateway/build/libs/api-gateway-1.0.0.jar`
- `auth-service/build/libs/auth-service-1.0.0.jar`
- `user-service/build/libs/user-service-1.0.0.jar`
- `product-service/build/libs/product-service-1.0.0.jar`
- `order-service/build/libs/order-service-1.0.0.jar`
- `inventory-service/build/libs/inventory-service-1.0.0.jar`
- `notification-service/build/libs/notification-service-1.0.0.jar`

---

## 🚀 Шаг 4: Запуск всех сервисов

### 4.1 Запустить весь стек

```bash
# Windows (PowerShell)
docker-compose up -d

# Linux/Mac (или через Make)
make dev-up
```

### 4.2 Проверить статус всех сервисов

```bash
docker-compose ps
```

**Ожидаемый результат:** Все сервисы в статусе `Up (healthy)` или `Up`

### 4.3 Проверить логи

```bash
# Все сервисы
docker-compose logs -f

# Конкретный сервис
docker-compose logs -f api-gateway
docker-compose logs -f auth-service
docker-compose logs -f order-service
```

**Ищите в логах:**
- ✅ `Started ApiGatewayApplication` (api-gateway)
- ✅ `Started AuthServiceApplication` (auth-service)
- ✅ `Started UserServiceApplication` (user-service)
- ✅ `Started ProductServiceApplication` (product-service)
- ✅ `Started OrderServiceApplication` (order-service)
- ✅ `Started InventoryServiceApplication` (inventory-service)
- ✅ `Started NotificationServiceApplication` (notification-service)

---

## ✅ Шаг 5: Проверка работоспособности

### 5.1 Health Checks (через API Gateway)

```bash
# Windows (PowerShell)
curl http://localhost:8080/actuator/health

# Linux/Mac
curl http://localhost:8080/actuator/health
```

**Ожидаемый результат:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 5.2 Проверка каждого сервиса напрямую

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Auth Service
curl http://localhost:8087/actuator/health

# User Service
curl http://localhost:8082/actuator/health

# Product Service
curl http://localhost:8083/actuator/health

# Order Service
curl http://localhost:8085/actuator/health

# Inventory Service
curl http://localhost:8084/actuator/health

# Notification Service
curl http://localhost:8086/actuator/health
```

### 5.3 Проверка Swagger UI

Откройте в браузере:
- **API Gateway Swagger:** http://localhost:8080/swagger-ui.html
- **Auth Service Swagger:** http://localhost:8087/swagger-ui.html
- **Order Service Swagger:** http://localhost:8085/swagger-ui.html

### 5.4 Проверка Kafka UI

Откройте в браузере:
- **Kafka UI:** http://localhost:8088

Проверьте наличие топиков:
- `order-platform.user.created`
- `order-platform.order.created`
- `order-platform.inventory.reserved`
- `order-platform.order.status-changed`

---

## 🧪 Шаг 6: Тестирование API

### 6.1 Регистрация пользователя

```bash
# Windows (PowerShell)
$body = @{
    email = "test@example.com"
    password = "SecurePass123!"
    firstName = "John"
    lastName = "Doe"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

# Linux/Mac
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Ожидаемый результат:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "message": "User registered successfully"
}
```

### 6.2 Логин и получение JWT токена

```bash
# Windows (PowerShell)
$loginBody = @{
    email = "test@example.com"
    password = "SecurePass123!"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$token = $response.token
Write-Host "JWT Token: $token"

# Linux/Mac
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }' | jq -r '.token')

echo "JWT Token: $TOKEN"
```

### 6.3 Получение списка товаров

```bash
# Windows (PowerShell)
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/products" `
    -Method GET `
    -Headers @{Authorization = "Bearer $token"}

# Linux/Mac
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

### 6.4 Создание заказа

```bash
# Windows (PowerShell)
$orderBody = @{
    items = @(
        @{productId = "1"; quantity = 2},
        @{productId = "2"; quantity = 1}
    )
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders" `
    -Method POST `
    -ContentType "application/json" `
    -Headers @{Authorization = "Bearer $token"} `
    -Body $orderBody

# Linux/Mac
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": "1", "quantity": 2},
      {"productId": "2", "quantity": 1}
    ]
  }'
```

---

## 🧪 Шаг 7: Запуск тестов

### 7.1 Unit тесты

```bash
# Windows (PowerShell)
.\gradlew test --parallel

# Linux/Mac
./gradlew test --parallel
```

**Ожидаемый результат:**
```
BUILD SUCCESSFUL in 3m 15s
```

### 7.2 Integration тесты

```bash
# Windows (PowerShell)
.\gradlew test --tests '*IntegrationTest' --parallel

# Linux/Mac
./gradlew test --tests '*IntegrationTest' --parallel
```

### 7.3 Contract тесты

```bash
# Windows (PowerShell)
.\gradlew :order-service:contractTest :product-service:contractTest

# Linux/Mac
./gradlew :order-service:contractTest :product-service:contractTest
```

### 7.4 Проверка покрытия кода

```bash
# Windows (PowerShell)
.\gradlew test jacocoTestReport

# Linux/Mac
./gradlew test jacocoTestReport
```

**Просмотр отчёта:**
```bash
# Windows (PowerShell)
Start-Process "order-service\build\reports\jacoco\test\html\index.html"

# Linux/Mac
open order-service/build/reports/jacoco/test/html/index.html
# или
xdg-open order-service/build/reports/jacoco/test/html/index.html
```

### 7.5 Проверка минимального покрытия (≥80%)

```bash
# Windows (PowerShell)
.\gradlew jacocoTestCoverageVerification

# Linux/Mac
./gradlew jacocoTestCoverageVerification
```

---

## 🔍 Шаг 8: Проверка Kafka событий

### 8.1 Проверить топики Kafka

```bash
# Войти в контейнер Kafka
docker exec -it kafka bash

# Список топиков
kafka-topics --list --bootstrap-server localhost:29092

# Просмотр сообщений в топике
kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic order-platform.order.created \
  --from-beginning
```

### 8.2 Проверить через Kafka UI

1. Откройте http://localhost:8088
2. Выберите топик `order-platform.order.created`
3. Нажмите "View Messages"
4. Создайте заказ через API и проверьте появление события

---

## 🗄️ Шаг 9: Проверка баз данных

### 9.1 PostgreSQL

```bash
# Войти в PostgreSQL
docker exec -it postgres psql -U postgres

# Список баз данных
\l

# Подключиться к базе
\c order_db

# Список таблиц
\dt

# Проверить заказы
SELECT * FROM orders LIMIT 5;
```

### 9.2 MongoDB

```bash
# Войти в MongoDB
docker exec -it mongodb mongosh -u admin -p ${MONGO_INITDB_ROOT_PASSWORD}

# Список баз данных
show dbs

# Использовать базу
use notifications

# Список коллекций
show collections

# Проверить уведомления
db.notification_logs.find().limit(5)
```

### 9.3 Redis

```bash
# Войти в Redis
docker exec -it redis redis-cli

# Проверить ключи
KEYS *

# Получить значение
GET <key>
```

---

## 🛑 Шаг 10: Остановка сервисов

### 10.1 Остановить все сервисы

```bash
# Windows (PowerShell)
docker-compose down

# Linux/Mac (или через Make)
make dev-down
```

### 10.2 Остановить с удалением данных

```bash
# Windows (PowerShell)
docker-compose down -v

# Linux/Mac
make dev-down
# (Makefile уже включает -v)
```

---

## 🐛 Troubleshooting

### Проблема: Сервисы не стартуют

**Решение:**
```bash
# Проверить логи
docker-compose logs <service-name>

# Пересоздать контейнеры
docker-compose down -v
docker-compose up -d

# Проверить порты
netstat -ano | findstr :8080  # Windows
lsof -i :8080                  # Linux/Mac
```

### Проблема: База данных не создаётся

**Решение:**
```bash
# Проверить init скрипт
docker exec -it postgres ls -la /docker-entrypoint-initdb.d/

# Создать базы вручную
docker exec -it postgres psql -U postgres -c "CREATE DATABASE order_db;"
```

### Проблема: Kafka не работает

**Решение:**
```bash
# Проверить Zookeeper
docker-compose logs zookeeper

# Перезапустить Kafka
docker-compose restart kafka

# Проверить топики
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:29092
```

### Проблема: Тесты падают

**Решение:**
```bash
# Очистить кэш Gradle
.\gradlew clean

# Запустить тесты по одному модулю
.\gradlew :order-service:test

# Проверить Testcontainers
docker ps | grep testcontainers
```

---

## 📊 Чеклист полной проверки

- [ ] Все контейнеры запущены (`docker-compose ps`)
- [ ] Health checks проходят (`/actuator/health`)
- [ ] Swagger UI доступен
- [ ] Kafka UI доступен
- [ ] Регистрация пользователя работает
- [ ] Логин возвращает JWT токен
- [ ] Создание заказа работает
- [ ] Kafka события публикуются
- [ ] Unit тесты проходят
- [ ] Integration тесты проходят
- [ ] Покрытие кода ≥80%

---

## 🎉 Готово!

Если все проверки пройдены, проект полностью работоспособен!

**Следующие шаги:**
- Изучите [API Reference](API_REFERENCE.md)
- Посмотрите [Demo Guide](DEMO.md)
- Настройте [Kubernetes deployment](KUBERNETES.md)


