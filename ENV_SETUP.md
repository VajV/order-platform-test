# Настройка переменных окружения

## 🔐 Важно о безопасности

**КРИТИЧЕСКИ ВАЖНО:**
- `.env` файл содержит секреты и НЕ должен коммититься в Git!
- `.env.example` - это шаблон для команды разработки
- В production используйте **HashiCorp Vault**, **Kubernetes Secrets** или **AWS Secrets Manager**

---

## 📋 Быстрая настройка

### 1. Создайте `.env` файл

```bash
cp .env.example .env
```

### 2. Замените критичные значения

Откройте `.env` и **обязательно измените**:

#### PostgreSQL (обязательно)
```bash
POSTGRES_PASSWORD=your_very_strong_password_here
```

#### MongoDB (обязательно)
```bash
MONGO_INITDB_ROOT_PASSWORD=another_strong_password_here
```

#### JWT Secret (КРИТИЧНО!)
```bash
# ВАЖНО: Минимум 32 символа!
# Генерация:
JWT_SECRET=$(openssl rand -base64 32)

# Или на Windows PowerShell:
# [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

Вставьте сгенерированный секрет в `.env`:
```bash
JWT_SECRET=ваш_сгенерированный_секрет_здесь
```

#### Vault Token (если используете Vault)
```bash
VAULT_TOKEN=hvs.your_vault_root_token_here
```

---

## 🛠️ Генерация безопасных секретов

### Linux / macOS

```bash
# JWT Secret (32 байта)
openssl rand -base64 32

# Пароль PostgreSQL (24 символа, alphanumeric)
openssl rand -base64 24 | tr -d /=+ | cut -c1-24

# Пароль MongoDB (32 символа)
openssl rand -base64 32 | tr -d /=+
```

### Windows PowerShell

```powershell
# JWT Secret
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# Пароль PostgreSQL
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object {[char]$_})

# Пароль MongoDB
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
```

---

## 📝 Пример заполненного `.env` (для разработки)

```bash
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=Dev2024SecurePass!
POSTGRES_DB=postgres

AUTH_DB_NAME=auth_db
USER_DB_NAME=user_db
PRODUCT_DB_NAME=product_db
ORDER_DB_NAME=order_db
INVENTORY_DB_NAME=inventory_db

# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=MongoSecure2024!
MONGO_INITDB_DATABASE=notifications

# JWT (сгенерирован с помощью openssl rand -base64 32)
JWT_SECRET=Zx8kP2qW9mN3vB6hJ5tY1rL4cF7gK0sA3dX6uM9nV2qW5eR8tY1uI4oP7aS0dF3g
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Vault (для локальной разработки)
VAULT_URI=http://localhost:8200
VAULT_TOKEN=dev-root-token-12345

# Redis (можно оставить пустым для dev)
REDIS_PASSWORD=

# Email (для notification-service, можно оставить пустым для dev)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@your-company.com
MAIL_PASSWORD=

# Общие настройки
SPRING_PROFILES_ACTIVE=docker
LOG_LEVEL=INFO

# CORS (разделитель - запятая)
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=100

# Kafka (опционально)
KAFKA_AUTO_CREATE_TOPICS=true
KAFKA_REPLICATION_FACTOR=1
```

---

## ✅ Проверка конфигурации

### 1. Проверка `.env` файла

```bash
# Убедитесь, что файл существует
ls -la .env

# Проверьте, что он не в Git
git status .env  # должна быть ошибка "not found"
```

### 2. Проверка длины JWT_SECRET

```bash
# Linux/macOS
echo -n "$JWT_SECRET" | wc -c  # должно быть >= 32

# Windows PowerShell
$env:JWT_SECRET.Length  # должно быть >= 32
```

### 3. Тестовый запуск Docker Compose

```bash
# Проверка парсинга переменных
docker compose config | grep -E "(JWT_SECRET|POSTGRES_PASSWORD|MONGO.*PASSWORD)"

# Запуск инфраструктуры
docker compose -f docker-compose-infra.yml up -d

# Проверка подключения к PostgreSQL
docker exec -it postgres psql -U postgres -c "SELECT version();"

# Проверка подключения к MongoDB
docker exec -it mongodb mongosh -u admin -p "your_mongo_password" --eval "db.version()"
```

---

## 🚨 Production Setup

**В production НЕ используйте `.env` файл!**

### Вариант 1: HashiCorp Vault

```bash
# Сохраните секреты в Vault
vault kv put secret/auth-service \
  jwt.secret="your-production-secret" \
  db.password="production-db-password"

# В docker-compose.yml или Kubernetes укажите
VAULT_URI=https://vault.your-company.com
VAULT_ROLE_ID=your-app-role-id
VAULT_SECRET_ID=your-app-secret-id
```

### Вариант 2: Kubernetes Secrets

```bash
# Создайте Kubernetes Secret
kubectl create secret generic order-platform-secrets \
  --from-literal=jwt-secret="your-production-secret" \
  --from-literal=postgres-password="production-db-password" \
  --from-literal=mongo-password="production-mongo-password"

# Используйте в Deployment
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: order-platform-secrets
        key: jwt-secret
```

### Вариант 3: AWS Secrets Manager / Azure Key Vault

```bash
# Через Spring Cloud AWS
spring.cloud.aws.secretsmanager.enabled=true
spring.cloud.aws.secretsmanager.prefix=/secret/order-platform/
```

---

## 🔍 Troubleshooting

### Проблема: "JWT_SECRET not set"

**Причина:** Переменная не экспортирована или слишком короткая.

**Решение:**
```bash
# Проверьте .env
cat .env | grep JWT_SECRET

# Убедитесь, что длина >= 32
echo -n "your_jwt_secret_here" | wc -c

# Пересоздайте контейнеры
docker compose down
docker compose up -d
```

### Проблема: "Access denied for user 'postgres'"

**Причина:** Пароль в `.env` не совпадает с тем, что в контейнере.

**Решение:**
```bash
# Удалите volume PostgreSQL (ВНИМАНИЕ: удалит все данные!)
docker compose down -v
docker volume rm order-processing-platform_postgres_data

# Перезапустите с новым паролем
docker compose up -d postgres
```

### Проблема: "Could not connect to MongoDB"

**Причина:** Неверные учетные данные или порт занят.

**Решение:**
```bash
# Проверьте логи MongoDB
docker logs mongodb

# Проверьте переменные окружения
docker exec mongodb env | grep MONGO

# Пересоздайте контейнер
docker compose down mongodb
docker compose up -d mongodb
```

---

## 📚 Дополнительные ресурсы

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [HashiCorp Vault Integration](https://spring.io/projects/spring-vault)
- [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Kubernetes Secrets Best Practices](https://kubernetes.io/docs/concepts/configuration/secret/)

