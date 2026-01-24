package com.ecommerce.notification.repository;

import com.ecommerce.notification.model.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    List<NotificationLog> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime from);

    List<NotificationLog> findByEmailAndStatus(String email, NotificationLog.NotificationStatus status);

    List<NotificationLog> findTop10ByOrderByCreatedAtDesc();
}
