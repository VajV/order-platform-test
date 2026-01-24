// ========================================
// ИНИЦИАЛИЗАЦИЯ MongoDB для notification-service
// ========================================

db = db.getSiblingDB('notifications');

// Создать коллекции
db.createCollection('notification_templates');
db.createCollection('notification_logs');
db.createCollection('notification_settings');

// Вставить шаблоны уведомлений
db.notification_templates.insertMany([
    {
        eventType: 'order.created',
        subjectTemplate: 'Заказ #{{orderId}} создан',
        bodyTemplate: 'Ваш заказ на сумму {{totalAmount}} успешно создан и ожидает обработки.',
        enabled: true,
        createdAt: new Date()
    },
    {
        eventType: 'order.status-changed',
        subjectTemplate: 'Статус заказа #{{orderId}} изменён',
        bodyTemplate: 'Статус заказа изменён на: {{status}}.',
        enabled: true,
        createdAt: new Date()
    },
    {
        eventType: 'inventory.reserved',
        subjectTemplate: 'Товары зарезервированы для заказа #{{orderId}}',
        bodyTemplate: 'Инвентарь зарезервирован. Кол-во позиций: {{itemCount}}.',
        enabled: true,
        createdAt: new Date()
    },
    {
        eventType: 'payment.completed',
        subjectTemplate: 'Оплата заказа #{{orderId}} завершена',
        bodyTemplate: 'Оплата на сумму {{amount}} принята.',
        enabled: true,
        createdAt: new Date()
    }
]);

// Создать индексы
db.notification_templates.createIndex({ eventType: 1, enabled: 1 });
db.notification_logs.createIndex({ userId: 1, createdAt: -1 });
db.notification_logs.createIndex({ orderId: 1 });

print('MongoDB notifications database initialized successfully!');
