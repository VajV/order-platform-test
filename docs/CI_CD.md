# CI/CD Pipeline Documentation

## 🎯 Обзор

Проект использует GitHub Actions для автоматизации:
- Сборки и тестирования
- Contract Testing
- Security Scanning
- Docker Build & Push
- Релизов

## 📁 Структура Workflows

```
.github/
├── workflows/
│   ├── ci.yml          # Основной CI pipeline
│   ├── pr-check.yml    # Проверка Pull Requests
│   └── release.yml     # Релиз и публикация Docker images
└── dependabot.yml      # Автообновление зависимостей
```

## 🔄 CI Pipeline (`ci.yml`)

Запускается при:
- Push в `main`, `develop`, `feature/**`, `fix/**`
- Pull Request в `main`, `develop`

### Jobs

| Job | Описание | Время |
|-----|----------|-------|
| **build** | Сборка + Unit Tests | ~3 мин |
| **contract-tests** | Spring Cloud Contract | ~2 мин |
| **integration-tests** | Testcontainers | ~5 мин |
| **coverage** | JaCoCo + Codecov | ~3 мин |
| **security** | Trivy Scan | ~2 мин |
| **docker-build** | Build images (7 services) | ~10 мин |

### Диаграмма

```
┌─────────┐
│  build  │
└────┬────┘
     │
     ├───────────────┬──────────────┬─────────────┐
     │               │              │             │
     ▼               ▼              ▼             ▼
┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│contract │   │integration│   │ coverage │   │ security │
│ tests   │   │  tests    │   │          │   │          │
└────┬────┘   └─────┬─────┘   └────┬─────┘   └────┬─────┘
     │              │              │              │
     └──────────────┴──────────────┴──────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │docker-build │
                   │ (7 services)│
                   └─────────────┘
```

## 🔍 PR Check (`pr-check.yml`)

Оптимизированный pipeline для Pull Requests:

1. **validate** - Компиляция, форматирование
2. **test-affected** - Тесты только изменённых сервисов
3. **contract-check** - Проверка контрактов при изменениях

### Умное определение изменений

Pipeline автоматически определяет какие сервисы были изменены и запускает тесты только для них:

```yaml
- name: Test order-service
  if: steps.changed-files.outputs.order_any_changed == 'true'
  run: ./gradlew :order-service:test
```

## 🚀 Release (`release.yml`)

Запускается при создании Git тега `v*`:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Что происходит:

1. **Build & Test** - Полная сборка и тестирование
2. **Publish** - Push Docker images в GitHub Container Registry
3. **Release** - Создание GitHub Release с changelog

### Docker Images

```bash
# После релиза v1.0.0
docker pull ghcr.io/<owner>/order-platform-api-gateway:1.0.0
docker pull ghcr.io/<owner>/order-platform-auth-service:1.0.0
docker pull ghcr.io/<owner>/order-platform-user-service:1.0.0
docker pull ghcr.io/<owner>/order-platform-product-service:1.0.0
docker pull ghcr.io/<owner>/order-platform-order-service:1.0.0
docker pull ghcr.io/<owner>/order-platform-inventory-service:1.0.0
docker pull ghcr.io/<owner>/order-platform-notification-service:1.0.0
```

## 🔐 Security Scanning

### Trivy

Сканирует:
- Исходный код на уязвимости
- Docker images
- Зависимости

Результаты загружаются в GitHub Security tab (SARIF format).

### OWASP Dependency Check (опционально)

```yaml
- name: OWASP Dependency Check
  run: ./gradlew dependencyCheckAnalyze
```

## 📊 Code Coverage

### JaCoCo

Генерирует отчёты для каждого сервиса:
```
**/build/reports/jacoco/test/html/index.html
```

### Codecov Integration

Автоматическая загрузка в Codecov:
```yaml
- uses: codecov/codecov-action@v4
  with:
    files: '**/jacocoTestReport.xml'
```

## 🤖 Dependabot

Автоматические PR для обновления:
- **Gradle** зависимостей (еженедельно)
- **GitHub Actions** (еженедельно)
- **Docker** base images (еженедельно)

### Группировка

```yaml
groups:
  spring:
    patterns: ["org.springframework*"]
  testing:
    patterns: ["org.testcontainers*", "org.mockito*"]
```

## ⚙️ Настройка Secrets

Необходимые GitHub Secrets:

| Secret | Описание | Обязательный |
|--------|----------|--------------|
| `GITHUB_TOKEN` | Автоматический | ✅ |
| `CODECOV_TOKEN` | Для Codecov | ❌ |
| `DOCKER_USERNAME` | Docker Hub (если используете) | ❌ |
| `DOCKER_PASSWORD` | Docker Hub (если используете) | ❌ |

## 🏃 Локальный запуск

```bash
# Симуляция CI локально
./gradlew clean build

# Только тесты
./gradlew test

# Contract tests
./gradlew :order-service:contractTest

# Coverage report
./gradlew test jacocoTestReport
open order-service/build/reports/jacoco/test/html/index.html
```

## 📈 Badges

Добавьте в README.md:

```markdown
![CI](https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg)
![Coverage](https://codecov.io/gh/<owner>/<repo>/branch/main/graph/badge.svg)
![License](https://img.shields.io/github/license/<owner>/<repo>)
```

## 🔧 Troubleshooting

### Tests fail in CI but pass locally

1. Проверьте версию Java (должна быть 21)
2. Проверьте доступность Docker для Testcontainers
3. Увеличьте таймауты для integration tests

### Contract tests fail

1. Убедитесь что `BaseContractTest` настроен правильно
2. Проверьте что моки возвращают данные соответствующие контрактам
3. Проверьте синтаксис Groovy DSL

### Docker build fails

1. Проверьте что JAR файлы собраны (`./gradlew build`)
2. Проверьте путь к JAR в Dockerfile
3. Проверьте `.dockerignore`

