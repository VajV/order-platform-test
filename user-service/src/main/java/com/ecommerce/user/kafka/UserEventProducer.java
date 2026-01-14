package com.ecommerce.user.kafka;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {
    private static final String TOPIC = "user.created";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public void publishUserCreated(User user) {
        UserCreatedEvent event = UserCreatedEvent.newBuilder()
                .setUserId(user.getId())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setRoles(user.getRoles().stream()
                        .map(role -> role.getName().toString())  // ✅ ИСПРАВЛЕНО
                        .collect(Collectors.toList()))
                .setCreatedAt(user.getCreatedAt().format(FORMATTER))
                .build();

        CompletableFuture<SendResult<String, UserCreatedEvent>> future =
                kafkaTemplate.send(TOPIC, user.getEmail(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ User created event sent: userId={}, topic={}, partition={}, offset={}",
                        user.getId(),
                        TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send user created event: userId={}", user.getId(), ex);
            }
        });
    }
}
