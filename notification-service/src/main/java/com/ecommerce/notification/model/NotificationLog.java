package com.ecommerce.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String email;
    private String eventType;
    private String subject;
    private String body;
    private NotificationStatus status;
    private String errorMessage;

    private LocalDateTime sentAt;

    @Indexed
    private LocalDateTime createdAt;

    public enum NotificationStatus {
        SENT,
        FAILED,
        RATE_LIMITED,
        PENDING
    }
}
