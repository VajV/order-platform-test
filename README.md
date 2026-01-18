# 🛒 Order Processing Platform

Event-Driven микросервисная платформа для обработки заказов e-commerce магазина.

[![CI Pipeline](https://github.com/your-org/order-processing-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/order-processing-platform/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/your-org/order-processing-platform/branch/main/graph/badge.svg)](https://codecov.io/gh/your-org/order-processing-platform)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)

---

## 🎯 Возможности

- ✅ **7 микросервисов** с чистой архитектурой
- ✅ **Event-Driven** взаимодействие через Apache Kafka
- ✅ **REST API** для синхронных вызовов
- ✅ **JWT** аутентификация (Spring Security 6)
- ✅ **Testcontainers** интеграционные тесты
- ✅ **Contract Testing** с Spring Cloud Contract
- ✅ **CI/CD** GitHub Actions с автоматическим деплоем
- ✅ **Kubernetes-ready** с Helm charts
- ✅ **80%+ code coverage**

---

## 🏗️ Архитектура

```
                         ┌─────────────────┐
                         │   API Gateway   │◄─── HTTP/REST
                         │    (Port 8080)  │
                         └────────┬────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼───────┐  ┌──────────────▼──────────────┐  ┌───────▼───────┐
│  Auth Service │  │        User Service         │  │Product Service│
│  (Port 8081)  │  │        (Port 8082)          │  │  (Port 8083)  │
│   PostgreSQL  │  │        PostgreSQL           │  │    MongoDB    │
└───────────────┘  └─────────────────────────────┘  └───────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼───────┐  ┌──────────────▼──────────────┐  ┌───────▼───────┐
│ Order Service │◄────────────────────────────────►│Inventory Svc  │
│  (Port 8085)  │         Apache Kafka            │  (Port 8084)   │
│   PostgreSQL  │                                  │   PostgreSQL   │
└───────────────┘                                  └───────────────┘
        │
        │ Kafka Events
        ▼
┌───────────────┐
│ Notification  │
│    Service    │
│  (Port 8086)  │
│    MongoDB    │
└───────────────┘
```

---

## 🚀 Быстрый старт

### Требования

- **Java 21+**
- **Docker Desktop** (WSL 2 для Windows)
- **Make** или PowerShell 5+
- **k3d** (для Kubernetes, опционально)

### Локальная разработка (Docker Compose)

```bash
# Клонировать репозиторий
git clone https://github.com/your-org/order-processing-platform.git
cd order-processing-platform

# Скопировать .env
cp .env.example .env

# Запустить все сервисы
make dev-up

# Проверить статус
docker-compose ps

# API Gateway: http://localhost:8080
# Swagger UI:  http://localhost:8080/swagger-ui.html
# Kafka UI:    http://localhost:8088
```

### Kubernetes (k3d)

```bash
# Создать кластер
make k3d-up

# Установить Helm chart
make helm-install

# Проверить статус
make k8s-status

# Port-forward API Gateway
make port-forward
# → http://localhost:8080
```

---

## 🧪 Тестирование

```bash
# Запустить все тесты
make test

# Генерировать отчёт покрытия
make coverage

# Contract tests
make test-contract

# Только unit-тесты
./gradlew test

# Только integration-тесты
./gradlew test --tests '*IntegrationTest'
```

---

## 📊 Статистика проекта

| Метрика | Значение |
|---------|----------|
| Микросервисы | 7 |
| Unit tests | 75+ |
| Integration tests | 20+ |
| Controller tests | 30+ |
| Contract tests | 8 |
| Code coverage | 80%+ |
| Строк кода | ~15,000 |

---

## 🔧 Технологический стек

### Backend
- **Java 21** - язык программирования
- **Spring Boot 3.2** - фреймворк
- **Spring Security 6** - безопасность (JWT)
- **Spring Cloud Gateway** - API Gateway
- **Spring Cloud Contract** - contract testing

### Messaging & Data
- **Apache Kafka** - event streaming
- **PostgreSQL 15** - OLTP база данных
- **MongoDB 7** - NoSQL для каталога
- **Redis 7** - кэширование

### Infrastructure
- **Docker** - контейнеризация
- **Kubernetes (k3d)** - оркестрация
- **Helm 3** - package manager
- **GitHub Actions** - CI/CD

### Testing
- **JUnit 5** - unit тесты
- **Testcontainers** - integration тесты
- **MockMvc** - controller тесты
- **Mockito** - моки
- **AssertJ** - assertions

---

## 📖 Документация

- [Kubernetes Deployment](docs/KUBERNETES.md)
- [CI/CD Pipeline](docs/CI_CD.md)
- [Contract Testing](docs/CONTRACT_TESTING.md)
- [Security Changes](SECURITY_CHANGES.md)
- [Environment Setup](ENV_SETUP.md)
- [Demo Guide](docs/DEMO.md)

---

## 🔐 Безопасность

- ✅ JWT-токены с HS256/RS256 алгоритмом
- ✅ RBAC (ROLE_USER, ROLE_MANAGER, ROLE_ADMIN)
- ✅ Rate limiting (Redis)
- ✅ Security scanning (Trivy, OWASP)
- ✅ Secrets в .env (не в коде)

---

## 📝 API Примеры

### Регистрация пользователя
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Создание заказа
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": "PROD-001", "productName": "Laptop", "quantity": 1, "unitPrice": 999.99}
    ]
  }'
```

---

## 🎓 Жизненный цикл заказа

```
NEW → RESERVED → PAID → SHIPPED → COMPLETED
       ↓
    CANCELLED (если резерв не удался)
```

---

## 📁 Структура проекта

```
order-processing-platform/
├── api-gateway/              # Spring Cloud Gateway
├── auth-service/             # Аутентификация (JWT)
├── user-service/             # Управление пользователями
├── product-service/          # Каталог товаров (MongoDB)
├── order-service/            # Обработка заказов
├── inventory-service/        # Управление запасами
├── notification-service/     # Уведомления (Email, Kafka)
├── helm/                     # Kubernetes Helm charts
├── docs/                     # Документация
├── scripts/                  # Скрипты инициализации
├── docker-compose.yml        # Docker Compose (full stack)
├── docker-compose-infra.yml  # Только инфраструктура
├── Makefile                  # Команды разработки
└── .github/workflows/        # CI/CD pipelines
```

---

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/amazing-feature`)
3. Commit изменения (`git commit -m 'Add amazing feature'`)
4. Push в branch (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

---

## 📄 Лицензия

MIT License - см. [LICENSE](LICENSE) для деталей.

---

## 👤 Автор

**Your Name**
- GitHub: [@your-github](https://github.com/your-github)
- Email: your.email@example.com

---

## 🙏 Благодарности

- [Spring Team](https://spring.io/) за отличный фреймворк
- [Testcontainers](https://testcontainers.com/) за простоту интеграционного тестирования
- [Bitnami](https://bitnami.com/) за качественные Helm charts

---

⭐ **Поставьте звезду, если проект был полезен!**
