package com.ecommerce.user.kafka;

import com.ecommerce.user.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.user-created:user.created}")
    private String userCreatedTopic;

    public void sendUserCreatedEvent(Long userId, String email, String firstName, String lastName, java.util.List<String> roles) {
        // ✅ Используем Lombok @Builder вместо Avro newBuilder()
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Sending UserCreatedEvent: {}", event);

        CompletableFuture<SendResult<String, UserCreatedEvent>> future =
                kafkaTemplate.send(userCreatedTopic, userId.toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send UserCreatedEvent: {}", event, ex);
            } else {
                log.info("UserCreatedEvent sent successfully: {}", result.getRecordMetadata());
            }
        });
    }
}
