# Contract Testing Guide

## 🎯 Обзор

Contract Testing (тестирование контрактов) обеспечивает совместимость между микросервисами без необходимости запускать все сервисы одновременно.

### Преимущества:
- **Раннее обнаружение breaking changes** - до деплоя в production
- **Независимое тестирование** - consumer и producer тестируются отдельно
- **Документирование API** - контракты служат живой документацией
- **Быстрый feedback** - тесты выполняются за секунды

## 📦 Используемые технологии

- **Spring Cloud Contract** v4.1.0
- **Groovy DSL** для описания контрактов
- **WireMock** для stub-серверов
- **Rest Assured** для HTTP-тестов

## 🏗️ Архитектура контрактов

```
┌─────────────────┐     Contract      ┌─────────────────┐
│  order-service  │ ◄────────────────► │ inventory-service│
│   (Producer)    │                    │   (Consumer)    │
└─────────────────┘                    └─────────────────┘
        │                                      │
        │ Kafka Events                         │
        ▼                                      ▼
┌─────────────────┐                    ┌─────────────────┐
│notification-svc │                    │ product-service │
│   (Consumer)    │                    │   (Producer)    │
└─────────────────┘                    └─────────────────┘
```

## 📁 Структура файлов

```
service/
├── src/test/
│   ├── java/.../contract/
│   │   ├── BaseContractTest.java          # Базовый класс для REST контрактов
│   │   └── MessagingBaseContractTest.java # Базовый класс для Kafka контрактов
│   └── resources/contracts/
│       ├── orders/
│       │   ├── shouldReturnOrderById.groovy
│       │   ├── shouldCreateOrder.groovy
│       │   └── shouldReturnUserOrders.groovy
│       └── messaging/
│           ├── orderCreatedEvent.groovy
│           └── orderStatusChangedEvent.groovy
```

## 🔧 Настройка Producer (order-service)

### build.gradle.kts
```kotlin
plugins {
    id("org.springframework.cloud.contract") version "4.1.0"
}

dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    testImplementation("io.rest-assured:spring-mock-mvc:5.4.0")
}

contracts {
    testFramework.set(TestFramework.JUNIT5)
    baseClassForTests.set("com.orderplatform.order.contract.BaseContractTest")
    contractsDslDir.set(file("src/test/resources/contracts"))
}
```

### BaseContractTest.java
```java
@WebMvcTest(OrderController.class)
@WithMockUser(roles = "USER")
public abstract class BaseContractTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        // Setup mocks to return expected data
        when(orderService.getOrder(anyLong()))
            .thenReturn(createSampleOrder());
    }
}
```

## 📝 Написание контрактов (Groovy DSL)

### REST API контракт
```groovy
Contract.make {
    name "should return order by id"
    description "Returns order details when order exists"
    
    request {
        method GET()
        url "/api/v1/orders/1"
        headers {
            accept(applicationJson())
        }
    }
    
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            userId: 100,
            status: "NEW",
            totalPrice: 199.98
        ])
        bodyMatchers {
            jsonPath('$.id', byEquality())
            jsonPath('$.status', byRegex("NEW|RESERVED|PAID"))
        }
    }
}
```

### Kafka Event контракт
```groovy
Contract.make {
    name "should publish order created event"
    label "order_created"
    
    input {
        triggeredBy("publishOrderCreatedEvent()")
    }
    
    outputMessage {
        sentTo "order.created"
        body([
            orderId: $(anyNumber()),
            userId: $(anyNumber()),
            totalPrice: $(anyNumber()),
            timestamp: $(regex(".*"))
        ])
    }
}
```

## 🔧 Настройка Consumer (inventory-service)

### build.gradle.kts
```kotlin
dependencies {
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}
```

### Consumer Test
```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.ecommerce:order-service:+:stubs:8090",
    stubsMode = StubsMode.LOCAL
)
class OrderServiceContractTest {
    
    @Test
    void shouldReceiveOrderInExpectedFormat() {
        // Stub runner автоматически поднимает WireMock с контрактами
        // Ваш код взаимодействует с ним как с реальным сервисом
    }
}
```

## 🚀 Команды

### Генерация тестов и stubs
```bash
# Producer: генерирует тесты из контрактов
./gradlew :order-service:generateContractTests

# Producer: запускает контрактные тесты
./gradlew :order-service:contractTest

# Producer: публикует stubs в local Maven repo
./gradlew :order-service:publishToMavenLocal
```

### Запуск consumer тестов
```bash
# Consumer: запускает тесты с подключением к stubs
./gradlew :inventory-service:test
```

## 📊 Существующие контракты

### order-service (Producer)

| Контракт | Тип | Описание |
|----------|-----|----------|
| `shouldReturnOrderById` | REST | GET /api/v1/orders/{id} |
| `shouldCreateOrder` | REST | POST /api/v1/orders |
| `shouldReturnUserOrders` | REST | GET /api/v1/orders?userId={id} |
| `orderCreatedEvent` | Kafka | Событие создания заказа |
| `orderStatusChangedEvent` | Kafka | Событие изменения статуса |

### product-service (Producer)

| Контракт | Тип | Описание |
|----------|-----|----------|
| `shouldReturnProductById` | REST | GET /api/products/{id} |
| `shouldReturnAllProducts` | REST | GET /api/products |
| `shouldReturnProductsByCategory` | REST | GET /api/products/category/{id} |

## ⚠️ Правила работы с контрактами

### ✅ DO
- Добавляйте новые контракты при создании новых endpoints
- Используйте `bodyMatchers` для гибкой валидации
- Храните контракты в version control
- Запускайте контрактные тесты в CI/CD pipeline

### ❌ DON'T
- Не изменяйте существующие контракты без согласования с consumers
- Не удаляйте контракты без deprecation периода
- Не используйте жёсткие значения для timestamp и UUID

## 🔄 Workflow в CI/CD

```yaml
# GitHub Actions пример
jobs:
  contract-tests:
    steps:
      - name: Run Producer Contract Tests
        run: ./gradlew :order-service:contractTest
        
      - name: Publish Stubs
        run: ./gradlew :order-service:publishToMavenLocal
        
      - name: Run Consumer Tests
        run: ./gradlew :inventory-service:test
```

## 📚 Дополнительные ресурсы

- [Spring Cloud Contract Docs](https://docs.spring.io/spring-cloud-contract/reference/)
- [Consumer-Driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [Pact.io](https://pact.io/) - альтернативный инструмент

