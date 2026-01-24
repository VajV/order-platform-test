package com.ecommerce.notification.config;

import com.ecommerce.notification.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class NotificationTemplateInitConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public CommandLineRunner notificationTemplatesInitializer() {
        return args -> {
            Query nullEventTypeQuery = new Query(Criteria.where("eventType").is(null));
            mongoTemplate.remove(nullEventTypeQuery, NotificationTemplate.class);

            IndexOperations indexOps = mongoTemplate.indexOps(NotificationTemplate.class);
            Index partialIndex = new Index()
                    .on("eventType", org.springframework.data.domain.Sort.Direction.ASC)
                    .unique()
                    .sparse();
            indexOps.ensureIndex(partialIndex);

            if (mongoTemplate.count(new Query(), NotificationTemplate.class) == 0) {
                LocalDateTime now = LocalDateTime.now();

                List<NotificationTemplate> templates = List.of(
                        NotificationTemplate.builder()
                                .eventType("order.created")
                                .subjectTemplate("Your order #{{orderId}} has been created")
                                .bodyTemplate("Thank you for your order! Total: ${{totalAmount}}. We are processing it now.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("order.status-changed")
                                .subjectTemplate("Order #{{orderId}} status updated to {{status}}")
                                .bodyTemplate("Your order #{{orderId}} status is now: {{status}}.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("inventory.reserved")
                                .subjectTemplate("Items reserved for order #{{orderId}}")
                                .bodyTemplate("We have reserved {{itemCount}} item(s) for your order #{{orderId}}.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("payment.completed")
                                .subjectTemplate("Payment received for order #{{orderId}}")
                                .bodyTemplate("Payment of ${{amount}} successfully received for order #{{orderId}}.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("user.created")
                                .subjectTemplate("Welcome to our platform!")
                                .bodyTemplate("Hello {{username}}! Your account has been created successfully. Email: {{email}}")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("inventory.failed")
                                .subjectTemplate("Order #{{orderId}} - Inventory reservation failed")
                                .bodyTemplate("Unfortunately, we couldn't reserve items for your order #{{orderId}}. Reason: {{reason}}")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build()
                );

                mongoTemplate.insertAll(templates);
            }
        };
    }
}
