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
        type: 'ORDER_CREATED',
        channel: 'EMAIL',
        subject: 'Заказ #{{orderId}} создан',
        body: 'Ваш заказ на сумму {{totalPrice}} руб. успешно создан и ожидает обработки.',
        active: true,
        createdAt: new Date()
    },
    {
        type: 'ORDER_CONFIRMED',
        channel: 'EMAIL',
        subject: 'Заказ #{{orderId}} подтверждён',
        body: 'Ваш заказ подтверждён и будет доставлен {{expectedDelivery}}.',
        active: true,
        createdAt: new Date()
    },
    {
        type: 'ORDER_SHIPPED',
        channel: 'EMAIL',
        subject: 'Заказ #{{orderId}} отправлен',
        body: 'Ваш заказ отправлен. Трек-номер: {{trackingNumber}}',
        active: true,
        createdAt: new Date()
    },
    {
        type: 'ORDER_CANCELLED',
        channel: 'EMAIL',
        subject: 'Заказ #{{orderId}} отменён',
        body: 'Ваш заказ был отменён. Причина: {{reason}}',
        active: true,
        createdAt: new Date()
    }
]);

// Создать индексы
db.notification_templates.createIndex({ type: 1, channel: 1 });
db.notification_logs.createIndex({ userId: 1, createdAt: -1 });
db.notification_logs.createIndex({ orderId: 1 });

print('MongoDB notifications database initialized successfully!');
