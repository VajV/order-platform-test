package com.ecommerce.notification.repository;

import com.ecommerce.notification.model.NotificationTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByEventTypeAndEnabled(String eventType, boolean enabled);

    Optional<NotificationTemplate> findByEventType(String eventType);
}
