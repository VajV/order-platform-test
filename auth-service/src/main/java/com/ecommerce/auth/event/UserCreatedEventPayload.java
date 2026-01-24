package com.ecommerce.auth.event;

import java.time.Instant;

public record UserCreatedEventPayload(
        Long userId,
        String username,
        String email,
        Instant timestamp
) {}
