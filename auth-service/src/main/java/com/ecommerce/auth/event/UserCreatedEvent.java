package com.ecommerce.auth.event;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email
) {}
