# API Gateway

Единая точка входа для микросервисной платформы Order-Processing.

## Функции

- ✅ Маршрутизация на микросервисы (auth, user, product, order, notification)
- ✅ JWT валидация и проброс headers (X-User-Id, X-User-Roles)
- ✅ Rate limiting (100 req/min по IP) через Redis
- ✅ CORS для фронтенда
- ✅ Swagger UI агрегация (/swagger-ui.html)
- ✅ Health checks (/actuator/health)

## Быстрый старт

### Локально (требует Redis + микросервисы)
\`\`\`bash
cd api-gateway
./gradlew build
java -jar build/libs/api-gateway-0.0.1-SNAPSHOT.jar
\`\`\`

### Docker (из монорепо)
\`\`\`bash
docker-compose up api-gateway redis
\`\`\`

## Тестирование

### 1. Health check
\`\`\`bash
curl http://localhost:8080/actuator/health
\`\`\`

### 2. Маршруты
- Auth (без JWT): POST http://localhost:8080/auth/login
- Users (с JWT): GET http://localhost:8080/api/users
- Products (с JWT): GET http://localhost:8080/api/products
- Orders (с JWT): GET http://localhost:8080/api/orders
- Notifications (с JWT, admin): GET http://localhost:8080/api/notifications

### 3. Rate limiting (>100 req/min → 429)
\`\`\`bash
for i in {1..101}; do curl -H "Authorization: Bearer token" http://localhost:8080/api/products; done
\`\`\`

### 4. Swagger UI
\`\`\`
http://localhost:8080/swagger-ui.html
\`\`\`

## Конфигурация

Изменяйте в \`application.yml\`:
- \`server.port\` — порт gateway
- \`spring.redis.host\` — хост Redis
- \`jwt.secret\` — ключ для JWT
- \`spring.cloud.gateway.routes\` — маршруты на микросервисы