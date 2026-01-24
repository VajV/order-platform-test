package com.ecommerce.user.kafka;

import java.time.Instant;

public record AuthUserCreatedEventPayload(
        Long userId,
        String username,
        String email,
        Instant timestamp
) {}
