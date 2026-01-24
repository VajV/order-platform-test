# 📡 API Reference

Краткое описание всех эндпоинтов платформы.

## Аутентификация

Все защищённые эндпоинты требуют заголовок:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## Auth Service (`/api/auth`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| POST | `/register` | Регистрация | Public |
| POST | `/login` | Логин → JWT | Public |
| POST | `/refresh` | Обновить токен | Public |
| POST | `/logout` | Выход | USER+ |

**Пример регистрации:**
```json
POST /api/auth/register
{
  "username": "john",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

---

## User Service (`/api/users`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| GET | `/me` | Текущий профиль | USER+ |
| PUT | `/me` | Обновить профиль | USER+ |
| GET | `/{id}` | Профиль по ID | ADMIN |
| GET | `/` | Список пользователей | ADMIN |

---

## Product Service (`/api/products`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| GET | `/` | Каталог товаров | Public |
| GET | `/{id}` | Товар по ID | Public |
| GET | `/category/{cat}` | По категории | Public |
| GET | `/filter?minPrice=&maxPrice=` | Фильтр по цене | Public |
| POST | `/` | Создать товар | ADMIN |
| PUT | `/{id}` | Обновить товар | ADMIN |
| DELETE | `/{id}` | Удалить товар | ADMIN |
| PATCH | `/{id}/publish` | Опубликовать | ADMIN |
| PATCH | `/{id}/unpublish` | Снять с публикации | ADMIN |

**Пример создания товара:**
```json
POST /api/products
{
  "name": "Laptop",
  "description": "Gaming laptop",
  "price": 1299.99,
  "category": "Electronics"
}
```

---

## Order Service (`/api/orders`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| POST | `/` | Создать заказ | USER+ |
| GET | `/` | Мои заказы | USER+ |
| GET | `/{id}` | Заказ по ID | USER+ |
| POST | `/{id}/cancel?reason=` | Отменить | USER+ |
| PATCH | `/{id}/status` | Изменить статус | MANAGER+ |
| POST | `/{id}/demo-lifecycle` | **Demo:** NEW→RESERVED→PAID | ADMIN |

**Пример создания заказа:**
```json
POST /api/orders
{
  "items": [
    {
      "productId": "prod-001",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 1299.99
    }
  ]
}
```

**Жизненный цикл заказа:**
```
NEW → RESERVED → PAID → SHIPPED → COMPLETED
 ↓
CANCELLED
```

---

## Inventory Service (`/api/inventory`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| GET | `/{productId}` | Остатки товара | USER+ |
| POST | `/reserve` | Резервирование | INTERNAL |
| POST | `/release` | Освобождение | INTERNAL |
| PUT | `/{productId}` | Обновить остатки | ADMIN |

**gRPC (порт 9090):**
- `CheckAvailability` — проверка наличия
- `ReserveInventory` — резервирование
- `ReleaseReservation` — отмена резерва
- `GetInventory` — получить остатки

---

## Notification Service (`/api/v1/notifications`)

| Метод | Путь | Описание | Роли |
|-------|------|----------|------|
| GET | `/` | Мои уведомления | USER+ |
| GET | `/{id}` | Уведомление по ID | USER+ |
| PATCH | `/{id}/read` | Отметить прочитанным | USER+ |

---

## Kafka Topics

| Topic | Producer | Consumer | Описание |
|-------|----------|----------|----------|
| `user.created` | auth | user, notification | Новый пользователь |
| `order.created` | order | inventory, notification | Новый заказ |
| `order.status-changed` | order | notification | Смена статуса |
| `inventory.reserved` | inventory | order | Резерв успешен |
| `inventory.failed` | inventory | order | Резерв не удался |
| `payment.completed` | payment | order | Оплата прошла |
| `payment.failed` | payment | order | Оплата не удалась |

---

## HTTP коды ответов

| Код | Значение |
|-----|----------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request (валидация) |
| 401 | Unauthorized (нет токена) |
| 403 | Forbidden (нет прав) |
| 404 | Not Found |
| 409 | Conflict (дубликат) |
| 503 | Service Unavailable (circuit breaker) |
