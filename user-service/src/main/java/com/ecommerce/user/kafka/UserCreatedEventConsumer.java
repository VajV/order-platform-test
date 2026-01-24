package com.ecommerce.user.kafka;

import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventConsumer {

    private final UserService userService;

    @KafkaListener(topics = "${kafka.topics.user-created:user.created}")
    public void onUserCreated(AuthUserCreatedEventPayload payload) {
        log.info("Received user.created payload: userId={} email={}", payload.userId(), payload.email());
        userService.handleUserCreatedEvent(payload.userId(), payload.username(), payload.email(), payload.timestamp());
    }
}
