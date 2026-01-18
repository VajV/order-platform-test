# 🔐 Безопасность проекта - Изменения от 18.01.2026

## ✅ Что было исправлено

### 1. **Удалены хардкоженные секреты**

**Было (❌ НЕБЕЗОПАСНО):**
```yaml
# docker-compose.yml
environment:
  JWT_SECRET: "super-secret-key-hardcoded"
  POSTGRES_PASSWORD: "password123"
```

**Стало (✅ БЕЗОПАСНО):**
```yaml
# docker-compose.yml
environment:
  JWT_SECRET: ${JWT_SECRET}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

Все секреты теперь вынесены в `.env` файл, который:
- ✅ Добавлен в `.gitignore` (НЕ коммитится в Git)
- ✅ Имеет шаблон `.env.example` для команды разработки
- ✅ Содержит подсказки по генерации безопасных паролей

---

### 2. **Создан `.env.example` с документацией**

Файл содержит:
- ✅ Все необходимые переменные окружения
- ✅ Комментарии с описанием каждого параметра
- ✅ Инструкции по генерации JWT Secret (минимум 32 символа)
- ✅ Разделение на секции (PostgreSQL, MongoDB, JWT, Vault, Redis, Email)
- ✅ Дефолтные значения для локальной разработки

**Пример генерации JWT Secret:**
```bash
# Linux/macOS
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

---

### 3. **Обновлён `docker-compose.yml`**

**Добавлены переменные окружения:**

#### ✅ Все сервисы получили:
- `SPRING_PROFILES_ACTIVE` — управление профилями (dev, docker, prod)
- `LOG_LEVEL` — уровень логирования (TRACE, DEBUG, INFO, WARN, ERROR)
- `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` — время жизни токенов
- `VAULT_URI` / `VAULT_TOKEN` — опциональная интеграция с Vault

#### ✅ API Gateway:
```yaml
environment:
  RATE_LIMIT_ENABLED: ${RATE_LIMIT_ENABLED:-true}
  RATE_LIMIT_REQUESTS_PER_MINUTE: ${RATE_LIMIT_REQUESTS_PER_MINUTE:-100}
  ALLOWED_ORIGINS: ${ALLOWED_ORIGINS:-http://localhost:3000}
```

#### ✅ Notification Service:
```yaml
environment:
  MAIL_HOST: ${MAIL_HOST:-smtp.gmail.com}
  MAIL_PORT: ${MAIL_PORT:-587}
  MAIL_USERNAME: ${MAIL_USERNAME:-}
  MAIL_PASSWORD: ${MAIL_PASSWORD:-}
```

#### ✅ Inventory Service:
```yaml
environment:
  REDIS_PASSWORD: ${REDIS_PASSWORD:-}
```

#### ✅ Vault (опциональный):
```yaml
vault:
  profiles:
    - with-vault  # Запускается только при: docker compose --profile with-vault up
```

---

### 4. **Создан `docker-compose-infra.yml`**

Для локальной разработки — запускает **только инфраструктуру**:
- PostgreSQL
- MongoDB
- Redis
- Kafka + Zookeeper + Schema Registry
- Kafka UI
- Vault (опционально с `--profile with-vault`)

**Использование:**
```bash
# Запустить только инфраструктуру
docker compose -f docker-compose-infra.yml up -d

# Запустить микросервисы локально
./gradlew :auth-service:bootRun
./gradlew :api-gateway:bootRun
# и т.д.
```

---

### 5. **Обновлён `.gitignore`**

**Добавлены критичные секции:**
```gitignore
# Environment & Secrets
.env
.env.local
.env.*.local
secrets/
vault-data/

# Kubernetes
k8s/secrets/
*.key
*.pem
*.crt
```

---

### 6. **Созданы скрипты запуска**

#### ✅ `start.bat` (Windows CMD)
#### ✅ `start.sh` (Linux/macOS)
#### ✅ `start.ps1` (Windows PowerShell с цветным выводом)

**Функционал:**
1. Проверка наличия `.env` файла
2. Проверка установки Docker и Docker Compose
3. Меню выбора режима запуска:
   - Full stack (инфраструктура + все сервисы)
   - Infrastructure only (для локальной разработки)
   - With Vault (с HashiCorp Vault)
   - Stop all (остановка всех сервисов)
   - Rebuild (полная пересборка)
4. Вывод статуса сервисов и health check эндпоинтов

**Пример использования:**
```bash
# Windows
.\start.bat

# Linux/macOS
chmod +x start.sh
./start.sh

# PowerShell (с параметрами)
.\start.ps1 -Mode full
.\start.ps1 -Mode infra
.\start.ps1 -Mode stop
```

---

### 7. **Создана документация `ENV_SETUP.md`**

Подробное руководство по настройке окружения:
- ✅ Генерация безопасных секретов (Linux/macOS/Windows)
- ✅ Проверка конфигурации
- ✅ Примеры заполненного `.env` для разработки
- ✅ Production setup (Vault, Kubernetes Secrets, AWS Secrets Manager)
- ✅ Troubleshooting (решение типичных проблем)

---

### 8. **Обновлён `README.md`**

Добавлены секции:
- ✅ Подробная инструкция по настройке секретов
- ✅ Security Warning с рекомендациями
- ✅ Примеры получения JWT токенов
- ✅ Раздел Troubleshooting
- ✅ Информация о Swagger UI и документации API
- ✅ Команды для проверки health checks

---

## 🚀 Как начать работу (Quick Start)

### 1. Клонирование и настройка

```bash
git clone <repository-url>
cd order-processing-platform

# Создать .env файл
cp .env.example .env

# Сгенерировать JWT Secret
openssl rand -base64 32  # Linux/macOS

# Вставить в .env:
# JWT_SECRET=сгенерированный_секрет_здесь
```

### 2. Замените критичные пароли в `.env`

```bash
# ОБЯЗАТЕЛЬНО измените:
POSTGRES_PASSWORD=ваш_сильный_пароль
MONGO_INITDB_ROOT_PASSWORD=другой_сильный_пароль
JWT_SECRET=минимум_32_символа_сгенерированных_openssl
```

### 3. Запуск

```bash
# Windows
.\start.bat

# Linux/macOS
chmod +x start.sh
./start.sh

# PowerShell
.\start.ps1
```

---

## 🔐 Production Deployment

### ⚠️ НЕ используйте `.env` файл в production!

### Рекомендуемые решения:

#### 1. **HashiCorp Vault** (рекомендовано)
```bash
# Сохранить секреты в Vault
vault kv put secret/auth-service jwt.secret="production-secret"

# В docker-compose или K8s указать
VAULT_URI=https://vault.your-company.com
VAULT_ROLE_ID=app-role-id
```

#### 2. **Kubernetes Secrets**
```bash
# Создать Secret
kubectl create secret generic order-platform-secrets \
  --from-literal=jwt-secret="production-secret" \
  --from-literal=postgres-password="secure-db-password"

# В Deployment
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: order-platform-secrets
        key: jwt-secret
```

#### 3. **AWS Secrets Manager / Azure Key Vault**
```yaml
spring:
  cloud:
    aws:
      secretsmanager:
        enabled: true
        prefix: /secret/order-platform/
```

---

## 📊 Checklist безопасности

### ✅ Локальная разработка
- [x] `.env` файл создан и заполнен
- [x] `.env` добавлен в `.gitignore`
- [x] JWT Secret длина >= 32 символа
- [x] Пароли PostgreSQL/MongoDB изменены с дефолтных
- [x] Vault токен изменён (если используется)

### ✅ Staging/Production
- [ ] Используется Vault или Kubernetes Secrets
- [ ] JWT Secret генерируется отдельно для каждого окружения
- [ ] PostgreSQL/MongoDB используют strong passwords (16+ символов)
- [ ] HTTPS включен для всех внешних эндпоинтов
- [ ] Rate limiting настроен (API Gateway)
- [ ] CORS настроен только на реальные frontend домены
- [ ] Redis защищён паролем
- [ ] Kafka использует SASL/SSL (если prod)
- [ ] Логи не содержат секреты (проверить `logback-spring.xml`)

---

## 📁 Новые файлы

```
order-processing-platform/
├── .env                        # 🔒 Локальные секреты (НЕ коммитится)
├── .env.example                # ✅ Шаблон для команды
├── ENV_SETUP.md                # 📖 Руководство по настройке
├── docker-compose.yml          # 🐳 Полный стек
├── docker-compose-infra.yml    # 🐳 Только инфраструктура
├── start.bat                   # 🚀 Скрипт запуска (Windows)
├── start.sh                    # 🚀 Скрипт запуска (Linux/macOS)
├── start.ps1                   # 🚀 Скрипт запуска (PowerShell)
├── SECURITY_CHANGES.md         # 📄 Этот файл
└── README.md                   # 📖 Обновлённая документация
```

---

## 🎯 Следующие шаги (Roadmap)

### Приоритет 1 (Критично для production)
- [ ] Настроить HashiCorp Vault в production
- [ ] Включить HTTPS (TLS) для всех внешних эндпоинтов
- [ ] Настроить Kafka SASL/SSL authentication
- [ ] Добавить Redis AUTH password в production
- [ ] Настроить Rate Limiting на API Gateway
- [ ] Включить Spring Security CSRF protection

### Приоритет 2 (Observability)
- [ ] Интегрировать Prometheus + Grafana
- [ ] Настроить OpenTelemetry + Jaeger (distributed tracing)
- [ ] Добавить ELK Stack (Elasticsearch, Logstash, Kibana)
- [ ] Настроить Alerting (PagerDuty, Slack)

### Приоритет 3 (CI/CD)
- [ ] GitHub Actions для сборки и тестов
- [ ] SonarQube для анализа кода
- [ ] Trivy для сканирования Docker образов
- [ ] ArgoCD для GitOps deployment

### Приоритет 4 (Testing)
- [ ] Включить integration tests (сейчас отключены @Disabled)
- [ ] Добавить E2E тесты (Testcontainers + RestAssured)
- [ ] Покрытие тестами >= 80%
- [ ] Contract Testing (Pact или Spring Cloud Contract)

---

## 📞 Контакты и поддержка

Если возникли вопросы по безопасности:
- Создайте issue в репозитории
- Прочитайте `ENV_SETUP.md` (Troubleshooting секция)
- Проверьте логи: `docker compose logs -f [service-name]`

---

**Дата обновления:** 18 января 2026  
**Версия:** 1.0.0  
**Статус:** ✅ Security Best Practices Applied

