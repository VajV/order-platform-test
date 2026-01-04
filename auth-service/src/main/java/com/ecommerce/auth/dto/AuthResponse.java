package com.ecommerce.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserDto user
) {
    /**
     * Фабричный метод для удобного создания
     */
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserDto user
    ) {
        return new AuthResponse(accessToken, refreshToken, tokenType, expiresIn, user);
    }
}
