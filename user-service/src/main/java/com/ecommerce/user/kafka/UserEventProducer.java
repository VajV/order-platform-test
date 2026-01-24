package com.ecommerce.user.kafka;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topic.user-created:user.created}")
    private String userCreatedTopic;

    public void sendUserCreatedEvent(Long userId, String email, String firstName, String lastName, java.util.List<String> roles) {
        log.warn("UserEventProducer is disabled: auth-service is the source of user.created. Skipping publish for userId={}", userId);
    }
}
