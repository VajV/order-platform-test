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
            // 1. Clean up invalid documents with null eventType to avoid duplicate key on null
            Query nullEventTypeQuery = new Query(Criteria.where("eventType").is(null));
            mongoTemplate.remove(nullEventTypeQuery, NotificationTemplate.class);

            // 2. Ensure a unique index on eventType only for non-null values (partial index)
            IndexOperations indexOps = mongoTemplate.indexOps(NotificationTemplate.class);
            Index partialIndex = new Index()
                    .on("eventType", org.springframework.data.domain.Sort.Direction.ASC)
                    .unique();
            indexOps.ensureIndex(partialIndex);

            // 3. Seed default templates if collection is empty
            if (mongoTemplate.count(new Query(), NotificationTemplate.class) == 0) {
                LocalDateTime now = LocalDateTime.now();

                List<NotificationTemplate> templates = List.of(
                        NotificationTemplate.builder()
                                .eventType("ORDER_CREATED")
                                .subjectTemplate("Your order #{orderId} has been created")
                                .bodyTemplate("Thank you for your order! We are processing it now.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("ORDER_STATUS_CHANGED")
                                .subjectTemplate("Order #{orderId} status updated to {status}")
                                .bodyTemplate("Your order status is now {status}. Check your account for details.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("INVENTORY_RESERVED")
                                .subjectTemplate("Items reserved for your order #{orderId}")
                                .bodyTemplate("We have reserved the items for your order. Please complete payment.")
                                .enabled(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        NotificationTemplate.builder()
                                .eventType("PAYMENT_COMPLETED")
                                .subjectTemplate("Payment received for order #{orderId}")
                                .bodyTemplate("We have successfully received your payment. Preparing your shipment.")
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
