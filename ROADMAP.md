# 🎯 План развития Order Processing Platform

**Текущий статус:** 45% готовности к production  
**Дата:** 18 января 2026  
**Цель:** Довести проект до 100% production-ready за 4-6 спринтов

---

## 📊 Приоритеты

### 🔴 Приоритет 0: НЕМЕДЛЕННО (1-2 дня)
**Цель:** Проверить работоспособность после изменений безопасности

#### ✅ Действия:
1. **Тестирование локального запуска**
   ```bash
   # Шаг 1: Замените секреты в .env
   notepad .env
   # JWT_SECRET, POSTGRES_PASSWORD, MONGO_INITDB_ROOT_PASSWORD
   
   # Шаг 2: Запустите инфраструктуру
   .\start.ps1 -Mode infra
   
   # Шаг 3: Проверьте health checks
   docker compose ps
   docker logs postgres
   docker logs mongodb
   docker logs kafka
   ```

2. **Сборка всех микросервисов**
   ```bash
   # Полная сборка
   .\gradlew clean build
   
   # Проверка, что тесты проходят (без @Disabled)
   .\gradlew test --info
   ```

3. **Запуск полного стека в Docker**
   ```bash
   # Пересоборка JAR файлов
   .\gradlew clean build -x test
   
   # Пересоборка Docker образов
   docker compose build --no-cache
   
   # Запуск всех сервисов
   docker compose up -d
   
   # Проверка логов
   docker compose logs -f auth-service
   docker compose logs -f user-service
   ```

4. **Функциональное тестирование**
   ```bash
   # Регистрация пользователя
   curl -X POST http://localhost:8087/api/auth/register `
     -H "Content-Type: application/json" `
     -d '{"username":"test","email":"test@example.com","password":"Test123!"}'
   
   # Логин
   curl -X POST http://localhost:8087/api/auth/login `
     -H "Content-Type: application/json" `
     -d '{"username":"test","password":"Test123!"}'
   
   # Получение токена и проверка API Gateway
   # (сохраните TOKEN из ответа)
   curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/users/me
   ```

5. **Проверка Kafka**
   - Откройте Kafka UI: http://localhost:8090
   - Убедитесь, что топики созданы
   - Проверьте, что events публикуются

#### 📝 Результат:
- [ ] Все сервисы успешно стартуют
- [ ] JWT аутентификация работает
- [ ] Kafka events обрабатываются
- [ ] PostgreSQL миграции применяются
- [ ] MongoDB подключается

---

## 🔴 Приоритет 1: КРИТИЧНО ДЛЯ PRODUCTION (Спринт 1-2, 2-3 недели)

### 1.1 Включить и исправить отключенные тесты
**Проблема:** Много тестов отключены `@Disabled` (integration tests)

**Файлы для исправления:**
```
notification-service/src/test/java/
  ├── KafkaIntegrationTest.java          @Disabled
  ├── MongoDBIntegrationTest.java        @Disabled
  ├── RedisIntegrationTest.java          @Disabled
  ├── NotificationControllerTest.java    @Disabled
  └── NotificationServiceApplicationTests.java @Disabled

user-service/src/test/java/
  └── UserIntegrationTest.java           @Disabled

inventory-service/build.gradle.kts
  └── test { enabled = false }           ❌ Полностью отключены
```

**Задачи:**
1. Включить тесты по одному
2. Исправить конфигурацию Testcontainers
3. Добавить `application-test.yml` для тестового профиля
4. Убедиться, что `./gradlew clean build` проходит без ошибок

**Критерий успеха:**
```bash
./gradlew clean build  # ✅ BUILD SUCCESSFUL
./gradlew test         # ✅ Все тесты зелёные
```

---

### 1.2 Настроить HashiCorp Vault для секретов
**Проблема:** `.env` файл небезопасен для production

**Задачи:**
1. **Создать Vault configuration**
   ```bash
   # Файл: vault/policies/order-platform-policy.hcl
   path "secret/data/auth-service/*" {
     capabilities = ["read"]
   }
   path "secret/data/user-service/*" {
     capabilities = ["read"]
   }
   # ... для каждого сервиса
   ```

2. **Сохранить секреты в Vault**
   ```bash
   # Запустить Vault
   docker compose --profile with-vault up -d vault
   
   # Сохранить секреты
   docker exec -it vault sh
   vault login dev-root-token
   
   vault kv put secret/auth-service \
     jwt.secret="production-jwt-secret-32-chars" \
     db.password="secure-postgres-password"
   
   vault kv put secret/user-service \
     jwt.secret="production-jwt-secret-32-chars" \
     db.password="secure-postgres-password"
   
   # Повторить для всех сервисов
   ```

3. **Обновить `application.yml` каждого сервиса**
   ```yaml
   spring:
     cloud:
       vault:
         enabled: true
         uri: ${VAULT_URI:http://vault:8200}
         token: ${VAULT_TOKEN}
         kv:
           enabled: true
           backend: secret
           profile-separator: '/'
           default-context: ${spring.application.name}
   ```

4. **Создать тесты Vault интеграции**

**Критерий успеха:**
- [ ] Все сервисы читают секреты из Vault
- [ ] При изменении секрета в Vault сервисы обновляются (refresh)
- [ ] `.env` файл не содержит критичных секретов

---

### 1.3 Добавить HTTPS/TLS для всех endpoints
**Проблема:** HTTP небезопасен для production

**Задачи:**
1. **Сгенерировать self-signed сертификаты (для dev/staging)**
   ```bash
   # Скрипт: scripts/generate-certs.sh
   openssl req -x509 -newkey rsa:4096 \
     -keyout api-gateway-key.pem \
     -out api-gateway-cert.pem \
     -days 365 -nodes \
     -subj "/CN=localhost"
   ```

2. **Обновить `api-gateway/application.yml`**
   ```yaml
   server:
     port: 8443
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: ${SSL_KEYSTORE_PASSWORD}
       key-store-type: PKCS12
       key-alias: api-gateway
   ```

3. **Добавить HTTP → HTTPS redirect**

4. **Production:** Использовать Let's Encrypt или корпоративные сертификаты

**Критерий успеха:**
- [ ] API Gateway доступен на `https://localhost:8443`
- [ ] HTTP автоматически редиректит на HTTPS
- [ ] Swagger UI работает через HTTPS

---

### 1.4 Настроить Rate Limiting и Circuit Breaker
**Проблема:** Нет защиты от DDoS и cascading failures

**Задачи:**
1. **Rate Limiting на API Gateway (уже частично реализовано)**
   - Проверить конфигурацию Redis
   - Добавить дифференцированные лимиты по эндпоинтам
   - Добавить лимиты по IP и по пользователю

2. **Circuit Breaker (Resilience4j)**
   ```gradle
   // api-gateway/build.gradle.kts
   implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
   ```

   ```yaml
   # api-gateway/application.yml
   resilience4j:
     circuitbreaker:
       configs:
         default:
           slidingWindowSize: 10
           permittedNumberOfCallsInHalfOpenState: 3
           waitDurationInOpenState: 10s
           failureRateThreshold: 50
   ```

3. **Добавить Retry и Timeout policies**

**Критерий успеха:**
- [ ] Rate limiting работает (тест: 100+ запросов за минуту → 429 Too Many Requests)
- [ ] Circuit breaker открывается при 50% ошибок
- [ ] Fallback responses возвращаются при недоступности сервиса

---

### 1.5 Добавить CORS и Security Headers
**Проблема:** XSS, CSRF, clickjacking vulnerabilities

**Задачи:**
1. **Настроить CORS на API Gateway**
   ```yaml
   spring:
     cloud:
       gateway:
         globalcors:
           corsConfigurations:
             '[/**]':
               allowedOrigins: ${ALLOWED_ORIGINS}
               allowedMethods: GET,POST,PUT,DELETE,OPTIONS
               allowedHeaders: Authorization,Content-Type
               exposedHeaders: Authorization
               maxAge: 3600
   ```

2. **Добавить Security Headers**
   ```java
   // SecurityConfig.java
   http.headers()
       .contentSecurityPolicy("default-src 'self'")
       .and()
       .xssProtection()
       .and()
       .frameOptions().deny()
       .and()
       .httpStrictTransportSecurity()
           .maxAgeInSeconds(31536000)
           .includeSubDomains(true);
   ```

**Критерий успеха:**
- [ ] CORS работает только для указанных доменов
- [ ] Security headers присутствуют в response
- [ ] OWASP ZAP scan показывает Low/Medium risk

---

## 🟡 Приоритет 2: OBSERVABILITY (Спринт 3-4, 2-3 недели)

### 2.1 Настроить Prometheus + Grafana
**Цель:** Мониторинг метрик в реальном времени

**Задачи:**
1. **Добавить в `docker-compose.yml`**
   ```yaml
   prometheus:
     image: prom/prometheus:latest
     volumes:
       - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
     ports:
       - "9090:9090"
   
   grafana:
     image: grafana/grafana:latest
     ports:
       - "3000:3000"
     environment:
       GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
   ```

2. **Создать `prometheus.yml`**
   ```yaml
   scrape_configs:
     - job_name: 'spring-boot'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets:
           - 'api-gateway:8080'
           - 'auth-service:8087'
           - 'user-service:8081'
           # ... все сервисы
   ```

3. **Импортировать Grafana dashboards**
   - Spring Boot 2.1 Statistics (ID: 10280)
   - JVM (Micrometer) (ID: 4701)
   - PostgreSQL Database (ID: 9628)
   - Kafka Exporter Overview (ID: 7589)

**Критерий успеха:**
- [ ] Grafana показывает метрики всех сервисов
- [ ] Дашборды отображают: CPU, Memory, JVM Heap, HTTP requests, DB connections
- [ ] Alerting настроен для критичных метрик

---

### 2.2 Настроить OpenTelemetry + Jaeger (Distributed Tracing)
**Цель:** Трейсинг запросов между микросервисами

**Задачи:**
1. **Добавить OpenTelemetry в каждый сервис**
   ```gradle
   implementation("io.opentelemetry:opentelemetry-api:1.32.0")
   implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:1.32.0")
   ```

2. **Добавить Jaeger в `docker-compose.yml`**
   ```yaml
   jaeger:
     image: jaegertracing/all-in-one:latest
     ports:
       - "16686:16686"  # UI
       - "14268:14268"  # Collector
   ```

3. **Конфигурация в `application.yml`**
   ```yaml
   management:
     tracing:
       sampling:
         probability: 1.0  # 100% для dev, 0.1 для prod
     otlp:
       endpoint: http://jaeger:14268/api/traces
   ```

**Критерий успеха:**
- [ ] Jaeger UI показывает traces для всех сервисов
- [ ] Видны spans для Kafka producers/consumers
- [ ] Можно отследить полный путь запроса от API Gateway до БД

---

### 2.3 Настроить Centralized Logging (ELK Stack)
**Цель:** Агрегация логов со всех сервисов

**Задачи:**
1. **Добавить ELK в `docker-compose.yml`**
   ```yaml
   elasticsearch:
     image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
     environment:
       - discovery.type=single-node
     ports:
       - "9200:9200"
   
   logstash:
     image: docker.elastic.co/logstash/logstash:8.11.0
     volumes:
       - ./monitoring/logstash.conf:/usr/share/logstash/pipeline/logstash.conf
   
   kibana:
     image: docker.elastic.co/kibana/kibana:8.11.0
     ports:
       - "5601:5601"
   ```

2. **Настроить Logstash encoder в каждом сервисе**
   ```xml
   <!-- logback-spring.xml -->
   <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
     <destination>logstash:5000</destination>
     <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
   </appender>
   ```

3. **Создать Kibana dashboards**

**Критерий успеха:**
- [ ] Все логи доступны в Kibana
- [ ] Можно фильтровать по сервису, уровню, trace ID
- [ ] Alerting настроен для ERROR логов

---

## 🟢 Приоритет 3: TESTING & QUALITY (Спринт 5-6, 2-3 недели)

### 3.1 Увеличить покрытие тестами до 80%+
**Текущее состояние:** ~40% coverage

**Задачи:**
1. **Unit тесты для всех Service классов**
   ```java
   @ExtendWith(MockitoExtension.class)
   class UserServiceTest {
       @Mock private UserRepository userRepository;
       @Mock private KafkaProducer kafkaProducer;
       @InjectMocks private UserService userService;
       
       @Test
       void shouldCreateUser() { /* ... */ }
   }
   ```

2. **Integration тесты с Testcontainers**
   ```java
   @SpringBootTest
   @Testcontainers
   class UserServiceIntegrationTest {
       @Container
       static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
       
       @Container
       static KafkaContainer kafka = new KafkaContainer(
           DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
       );
   }
   ```

3. **E2E тесты через API Gateway**
   ```java
   @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
   class OrderFlowE2ETest {
       @Test
       void shouldCompleteOrderFlow() {
           // 1. Register user
           // 2. Login (get JWT)
           // 3. Create order
           // 4. Check inventory updated
           // 5. Check notification sent
       }
   }
   ```

4. **Contract Testing (Pact или Spring Cloud Contract)**

**Критерий успеха:**
- [ ] JaCoCo report показывает 80%+ coverage
- [ ] Все critical paths покрыты тестами
- [ ] `./gradlew clean build` всегда зелёный

---

### 3.2 Настроить Code Quality Tools
**Задачи:**
1. **SonarQube**
   ```yaml
   # docker-compose.yml
   sonarqube:
     image: sonarqube:community
     ports:
       - "9000:9000"
   ```

2. **Checkstyle, SpotBugs, PMD**
   ```gradle
   // build.gradle.kts
   plugins {
       id("checkstyle")
       id("com.github.spotbugs") version "5.0.14"
       id("pmd")
   }
   ```

3. **OWASP Dependency Check**
   ```gradle
   id("org.owasp.dependencycheck") version "8.4.0"
   ```

**Критерий успеха:**
- [ ] SonarQube Quality Gate = Passed
- [ ] 0 Critical/Blocker issues
- [ ] 0 High severity vulnerabilities

---

## 🔵 Приоритет 4: CI/CD & DEPLOYMENT (Спринт 7-8, 2-3 недели)

### 4.1 GitHub Actions CI/CD Pipeline
**Задачи:**
1. **Создать `.github/workflows/ci.yml`**
   ```yaml
   name: CI
   on: [push, pull_request]
   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-java@v4
           with:
             java-version: '21'
         - name: Build with Gradle
           run: ./gradlew clean build
         - name: Run tests
           run: ./gradlew test
         - name: Upload coverage to Codecov
           uses: codecov/codecov-action@v3
   ```

2. **Docker image build and push**
   ```yaml
   - name: Build Docker images
     run: |
       docker compose build
       docker tag auth-service:latest registry.com/auth-service:${{ github.sha }}
       docker push registry.com/auth-service:${{ github.sha }}
   ```

3. **Security scanning (Trivy)**
   ```yaml
   - name: Run Trivy vulnerability scanner
     uses: aquasecurity/trivy-action@master
     with:
       image-ref: auth-service:latest
       severity: 'CRITICAL,HIGH'
   ```

**Критерий успеха:**
- [ ] PR автоматически запускает CI
- [ ] Fail if tests fail или vulnerabilities found
- [ ] Docker images автоматически публикуются в registry

---

### 4.2 Kubernetes Deployment
**Задачи:**
1. **Создать Kubernetes manifests**
   ```bash
   k8s/
   ├── namespace.yaml
   ├── configmaps/
   ├── secrets/
   ├── deployments/
   │   ├── auth-service.yaml
   │   ├── user-service.yaml
   │   └── ...
   ├── services/
   ├── ingress.yaml
   └── hpa.yaml  # Horizontal Pod Autoscaler
   ```

2. **Helm Chart**
   ```bash
   helm create order-platform
   # Настроить values.yaml для dev/staging/prod
   ```

3. **ArgoCD для GitOps**
   ```yaml
   # argocd/application.yaml
   apiVersion: argoproj.io/v1alpha1
   kind: Application
   metadata:
     name: order-platform
   spec:
     source:
       repoURL: https://github.com/your-org/order-platform
       targetRevision: main
       path: k8s/
   ```

**Критерий успеха:**
- [ ] Все сервисы деплоятся в Kubernetes
- [ ] Health checks, readiness/liveness probes настроены
- [ ] ArgoCD автоматически синхронизирует изменения

---

## 📅 Timeline Summary

| Спринт | Недели | Приоритет | Задачи | Статус |
|--------|--------|-----------|--------|--------|
| **0** | 1-2 дня | 🔴 P0 | Тестирование текущих изменений | 🟡 В процессе |
| **1** | 1-2 | 🔴 P1 | Включить тесты, Vault | ⚪ Не начато |
| **2** | 3-4 | 🔴 P1 | HTTPS, Rate Limiting, Security Headers | ⚪ Не начато |
| **3** | 5-6 | 🟡 P2 | Prometheus, Grafana | ⚪ Не начато |
| **4** | 7-8 | 🟡 P2 | Jaeger, ELK Stack | ⚪ Не начато |
| **5** | 9-10 | 🟢 P3 | Unit/Integration тесты 80%+ | ⚪ Не начато |
| **6** | 11-12 | 🟢 P3 | SonarQube, Code Quality | ⚪ Не начато |
| **7** | 13-14 | 🔵 P4 | GitHub Actions CI/CD | ⚪ Не начато |
| **8** | 15-16 | 🔵 P4 | Kubernetes, ArgoCD | ⚪ Не начато |

**Итого:** 16 недель (4 месяца) до 100% production-ready

---

## 🎯 Definition of Done (Production Ready)

### ✅ Безопасность
- [x] Нет хардкоженных секретов
- [ ] Vault настроен для production
- [ ] HTTPS для всех endpoints
- [ ] Rate limiting включен
- [ ] Security headers настроены
- [ ] OWASP Top 10 vulnerabilities закрыты

### ✅ Observability
- [ ] Prometheus + Grafana (метрики)
- [ ] Jaeger (distributed tracing)
- [ ] ELK Stack (centralized logging)
- [ ] Alerting настроен (PagerDuty/Slack)

### ✅ Testing
- [ ] Unit tests coverage >= 80%
- [ ] Integration tests (Testcontainers)
- [ ] E2E tests
- [ ] Contract tests
- [ ] Performance tests (JMeter/Gatling)

### ✅ Code Quality
- [ ] SonarQube Quality Gate = Passed
- [ ] 0 Critical/Blocker issues
- [ ] Checkstyle/SpotBugs configured
- [ ] 0 High severity vulnerabilities (OWASP Dependency Check)

### ✅ CI/CD
- [ ] GitHub Actions pipeline
- [ ] Automated tests on PR
- [ ] Docker images автоматически публикуются
- [ ] Security scanning (Trivy)

### ✅ Deployment
- [ ] Kubernetes manifests
- [ ] Helm charts
- [ ] ArgoCD GitOps
- [ ] Health checks, readiness/liveness probes
- [ ] Horizontal Pod Autoscaling

### ✅ Documentation
- [x] README.md актуален
- [x] API документация (Swagger)
- [ ] Architecture Decision Records (ADR)
- [ ] Runbooks для production incidents
- [ ] Disaster Recovery Plan

---

## 📞 Следующий шаг

**Немедленно выполните Приоритет 0:**
```bash
# 1. Замените секреты
notepad .env

# 2. Запустите инфраструктуру
.\start.ps1 -Mode infra

# 3. Соберите проект
.\gradlew clean build

# 4. Запустите полный стек
.\start.ps1 -Mode full

# 5. Проверьте health checks
docker compose ps
Invoke-WebRequest http://localhost:8080/actuator/health
```

**После успешного запуска — начинайте Спринт 1 (Приоритет 1).**

---

**Автор:** AI Assistant  
**Дата:** 18 января 2026  
**Версия:** 1.0.0

