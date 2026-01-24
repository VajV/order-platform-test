package com.ecommerce.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-created}")
    private String userCreatedTopic;

    public void send(UserCreatedEvent event) {
        UserCreatedEventPayload payload = new UserCreatedEventPayload(
                event.userId(),
                event.username(),
                event.email(),
                Instant.now()
        );

        kafkaTemplate.send(userCreatedTopic, String.valueOf(event.userId()), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user.created for userId={}: {}", event.userId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Published user.created for userId={} to topic={}", event.userId(), userCreatedTopic);
                    }
                });
    }
}
