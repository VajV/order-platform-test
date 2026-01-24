# 🎬 Demo Guide

## Подготовка к демо

### 1. Запуск системы

```bash
# Вариант A: Docker Compose (простой)
make dev-up

# Вариант B: Kubernetes (полный)
make k3d-up
make helm-install
make port-forward
```

### 2. Проверка готовности

```bash
# Проверить все сервисы
curl http://localhost:8080/actuator/health

# Ожидаемый результат: {"status":"UP"}

# Проверить все контейнеры
docker-compose ps
```

---

## 📋 Сценарий демо (5 минут)

### Минута 1: Архитектура (30 сек)

1. Показать схему 7 микросервисов в README
2. Объяснить event-driven подход (Kafka)
3. Упомянуть технологии: Spring Boot 3, Java 21, PostgreSQL, MongoDB

### Минута 2: Запуск системы (30 сек)

```bash
# Одна команда - вся инфраструктура
make dev-up

# Показать Docker Compose output
docker-compose ps

# Ожидаемый вывод: 10+ контейнеров running
```

### Минута 3: API Gateway + Swagger (1 мин)

1. Открыть http://localhost:8080/swagger-ui.html
2. Показать все доступные endpoints по сервисам
3. Выполнить регистрацию через Swagger UI:
   - POST `/api/users/register`
   - Показать успешный response
4. Получить JWT токен:
   - POST `/api/auth/login`
   - Скопировать токен

### Минута 4: Полный цикл заказа (1.5 мин)

**Используя demo.http или Swagger UI:**

1. **Каталог товаров:**
   ```bash
   curl http://localhost:8080/api/products
   ```

2. **Создание заказа:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/orders \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"userId": 1, "items": [{"productId": "PROD-001", "productName": "Laptop", "quantity": 1, "unitPrice": 999.99}]}'
   ```

3. **Показать Kafka events:**
   - Открыть http://localhost:8088 (Kafka UI)
   - Показать topics: `order.created`, `order.status-changed`
   - Показать сообщения в топике

4. **Проверить статус заказа:**
   ```bash
   curl http://localhost:8080/api/v1/orders/1 \
     -H "Authorization: Bearer <TOKEN>"
   ```

### Минута 5: Тесты + CI/CD (1 мин)

```bash
# Запустить тесты
make test

# Показать статистику
# > 130+ tests, 80%+ coverage

# Открыть coverage report
open order-service/build/reports/jacoco/test/html/index.html

# Показать GitHub Actions
# → github.com/your-repo/actions
# → Показать зелёный pipeline
```

### Бонус: Kubernetes (если время есть)

```bash
# Показать поды
kubectl get pods -n order-platform-dev

# Показать сервисы
kubectl get svc -n order-platform-dev

# Масштабирование
kubectl scale deployment order-platform-order-service \
  --replicas=3 -n order-platform-dev

# Проверить масштабирование
kubectl get pods -n order-platform-dev -w
```

---

## 🎥 Структура видео (5-7 минут)

### 1. Intro (20 сек)
- Название: "Order Processing Platform"
- Технологии: Java 21, Spring Boot 3, Kafka, PostgreSQL
- Цель: Event-driven микросервисная архитектура

### 2. Code Walkthrough (1 мин)
- Структура проекта в IDE
- Открыть `order-service`:
  - `OrderController.java` - REST endpoints
  - `OrderService.java` - бизнес-логика
  - `KafkaEventPublisher.java` - Kafka producer
- Показать контракты в `contracts/orders/`

### 3. Live Demo (3 мин)
- Следовать сценарию выше
- Показать end-to-end flow от регистрации до заказа
- Kafka UI с событиями

### 4. Testing (1 мин)
```bash
make test
```
- Показать output: 130+ tests passed
- Открыть JaCoCo report
- Показать coverage: 80%+
- Показать GitHub Actions CI status

### 5. Deployment (1 мин)
```bash
make k8s-status
```
- Показать Kubernetes pods
- Показать Helm values
- Объяснить values-dev vs values-prod

### 6. Outro (20 сек)
- Итоги: 7 сервисов, 130+ тестов, K8s ready
- GitHub ссылка
- "Спасибо за внимание!"

---

## 🛠️ Инструменты для записи

| Инструмент | Назначение | Ссылка |
|------------|------------|--------|
| **OBS Studio** | Запись экрана (бесплатно) | [obsproject.com](https://obsproject.com/) |
| **Loom** | Быстрые скринкасты | [loom.com](https://loom.com/) |
| **Kdenlive** | Видеомонтаж (бесплатно) | [kdenlive.org](https://kdenlive.org/) |
| **Canva** | Титры и заставки | [canva.com](https://canva.com/) |

---

## ✅ Чеклист перед записью

```
□ Docker Desktop запущен
□ Все контейнеры в статусе running (docker-compose ps)
□ API Gateway отвечает (curl localhost:8080/actuator/health)
□ Swagger UI открыт (localhost:8080/swagger-ui.html)
□ Kafka UI открыт (localhost:8088)
□ demo.http готов в VS Code
□ GitHub Actions последний build зелёный
□ Coverage report сгенерирован
□ IDE открыта на нужных файлах
□ Микрофон проверен
□ Экран без лишних окон
```

---

## 📊 Ключевые метрики для демо

| Метрика | Значение |
|---------|----------|
| Микросервисы | 7 |
| Unit tests | 75+ |
| Integration tests | 20+ |
| Controller tests | 30+ |
| Contract tests | 8 |
| Total tests | **130+** |
| Code coverage | **80%+** |
| Kafka topics | 5 |
| Docker images | 7 |
| K8s templates | 14 |

---

## 🐛 Troubleshooting

### Проблема: Сервисы не стартуют

```bash
# Проверить логи
docker-compose logs -f

# Пересоздать контейнеры
make dev-down && make dev-up

# Проверить .env файл
cat .env
```

### Проблема: Тесты падают

```bash
# Очистить кэш
./gradlew clean

# Запустить по одному модулю
./gradlew :order-service:test --info
```

### Проблема: Kafka не коннектится

```bash
# Проверить Kafka
docker exec -it postgres psql -U postgres -c "\l"

# Проверить топики
docker exec -it $(docker ps -q -f name=kafka) \
  kafka-topics --list --bootstrap-server localhost:9092
```

### Проблема: База данных не создана

```bash
# Проверить PostgreSQL
docker exec -it postgres psql -U postgres -c "\l"

# Пересоздать с нуля
docker-compose down -v
docker-compose up -d
```

---

## 🎯 Quick Commands

```bash
# Полный demo flow
make dev-up                    # 1. Запуск
open http://localhost:8080     # 2. API Gateway
open http://localhost:8088     # 3. Kafka UI
make test                      # 4. Тесты
make coverage                  # 5. Coverage
```

---

## 📝 Скрипт для терминала

```bash
#!/bin/bash
# demo.sh - Автоматический запуск demo

echo "🚀 Starting Order Platform Demo..."

# Запуск
echo "📦 Starting services..."
make dev-up
sleep 30

# Проверка
echo "✅ Checking health..."
curl -s http://localhost:8080/actuator/health | jq

# Открыть браузеры
echo "🌐 Opening browsers..."
open http://localhost:8080/swagger-ui.html
open http://localhost:8088

echo "✨ Demo ready! Press Enter to run tests..."
read

# Тесты
echo "🧪 Running tests..."
make test

echo "🎉 Demo complete!"
```

---

**Удачи с демо! 🚀**

