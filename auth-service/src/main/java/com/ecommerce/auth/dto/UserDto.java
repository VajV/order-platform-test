package com.ecommerce.auth.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}