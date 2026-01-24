package com.ecommerce.notification.config;

import com.ecommerce.notification.model.NotificationTemplate;
import com.ecommerce.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoDBInitializer {

    @Bean
    CommandLineRunner initMongoDatabase(NotificationTemplateRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("📦 MongoDB templates already initialized. Count: {}", repository.count());
                return;
            }

            log.info("🔄 Initializing MongoDB notification templates...");

            List<NotificationTemplate> templates = Arrays.asList(

                    // Template 1: Order Created
                    NotificationTemplate.builder()
                            .eventType("order.created")
                            .subjectTemplate("Ваш заказ №{{orderId}} создан")
                            .bodyTemplate("""
                        <html>
                        <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <h2 style="color: #2c3e50;">🎉 Спасибо за заказ!</h2>
                                <p>Ваш заказ <strong style="color: #3498db;">№{{orderId}}</strong> успешно создан и принят в обработку.</p>
                                <div style="background-color: #ecf0f1; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                    <p style="margin: 5px 0;"><strong>Сумма заказа:</strong> {{totalAmount}} руб.</p>
                                </div>
                                <p>Мы начали обработку вашего заказа. Следите за обновлениями статуса в личном кабинете.</p>
                                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                                <p style="color: #7f8c8d; font-size: 12px; text-align: center;">
                                    © 2026 E-Commerce Platform. Все права защищены.
                                </p>
                            </div>
                        </body>
                        </html>
                        """)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build(),

                    // Template 2: Order Status Changed
                    NotificationTemplate.builder()
                            .eventType("order.status-changed")
                            .subjectTemplate("Заказ {{orderId}}: статус изменён на «{{status}}»")
                            .bodyTemplate("""
                        <html>
                        <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <h2 style="color: #2c3e50;">📦 Обновление статуса заказа</h2>
                                <p>Заказ <strong style="color: #3498db;">№{{orderId}}</strong></p>
                                <div style="background-color: #e8f8f5; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #1abc9c;">
                                    <p style="margin: 0;"><strong style="color: #16a085;">Новый статус:</strong> {{status}}</p>
                                </div>
                                <p>Спасибо за использование нашего сервиса!</p>
                                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                                <p style="color: #7f8c8d; font-size: 12px; text-align: center;">
                                    © 2026 E-Commerce Platform. Все права защищены.
                                </p>
                            </div>
                        </body>
                        </html>
                        """)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build(),

                    // Template 3: Inventory Reserved
                    NotificationTemplate.builder()
                            .eventType("inventory.reserved")
                            .subjectTemplate("Товары для заказа {{orderId}} зарезервированы")
                            .bodyTemplate("""
                        <html>
                        <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <h2 style="color: #2c3e50;">✅ Товары зарезервированы</h2>
                                <p>Для заказа <strong style="color: #3498db;">№{{orderId}}</strong> успешно зарезервировано <strong>{{itemCount}}</strong> товаров.</p>
                                <div style="background-color: #fff9e6; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #f39c12;">
                                    <p style="margin: 0;">⏰ Товары зарезервированы на 30 минут. Пожалуйста, завершите оплату.</p>
                                </div>
                                <p>Ожидаем вашей оплаты для завершения заказа.</p>
                                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                                <p style="color: #7f8c8d; font-size: 12px; text-align: center;">
                                    © 2026 E-Commerce Platform. Все права защищены.
                                </p>
                            </div>
                        </body>
                        </html>
                        """)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build(),

                    // Template 4: Payment Completed
                    NotificationTemplate.builder()
                            .eventType("payment.completed")
                            .subjectTemplate("✅ Оплата заказа {{orderId}} выполнена успешно!")
                            .bodyTemplate("""
                        <html>
                        <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                                <h2 style="color: #27ae60;">💳 Спасибо за оплату!</h2>
                                <p>Оплата за заказ <strong style="color: #3498db;">№{{orderId}}</strong> успешно выполнена.</p>
                                <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #28a745;">
                                    <p style="margin: 5px 0;"><strong>Сумма платежа:</strong> {{amount}} руб.</p>
                                    <p style="margin: 5px 0; color: #155724;">✅ Платёж обработан успешно</p>
                                </div>
                                <p>Ваш заказ упакован и скоро будет отправлен. Номер для отслеживания будет отправлен отдельным письмом.</p>
                                <p style="margin-top: 30px;">Хорошего дня! 🚀</p>
                                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                                <p style="color: #7f8c8d; font-size: 12px; text-align: center;">
                                    © 2026 E-Commerce Platform. Все права защищены.
                                </p>
                            </div>
                        </body>
                        </html>
                        """)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()
            );

            repository.saveAll(templates);
            log.info("✅ Successfully initialized {} notification templates", templates.size());

            // Вывести список созданных шаблонов
            templates.forEach(t ->
                    log.info("  - {}: {}", t.getEventType(), t.getSubjectTemplate())
            );
        };
    }
}
