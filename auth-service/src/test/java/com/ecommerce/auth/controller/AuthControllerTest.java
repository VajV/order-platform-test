package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserDto;
import com.ecommerce.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse authResponse;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserDto userDto = new UserDto(
                1L, "testuser", "test@example.com", "Test User",
                "ROLE_USER", LocalDateTime.now().minusDays(1), LocalDateTime.now()
        );

        authResponse = new AuthResponse(
                "access_token_value",
                "refresh_token_value",
                "Bearer", 900L, userDto
        );

        registerRequest = new RegisterRequest("test", "test@test.com", "Pass123456", "Test");
        loginRequest = new LoginRequest("test", "Pass123");
    }

    @Test
    @DisplayName("Test register returns auth response")
    void testRegister() {
        when(authService.register(any())).thenReturn(authResponse);

        var result = authController.register(registerRequest);

        assertNotNull(result);
        assertEquals("Bearer", result.getBody().tokenType());
        verify(authService).register(any());
    }

    @Test
    @DisplayName("Test login returns auth response")
    void testLogin() {
        when(authService.login(any())).thenReturn(authResponse);

        var result = authController.login(loginRequest);

        assertNotNull(result);
        assertEquals("Bearer", result.getBody().tokenType());
        verify(authService).login(any());
    }

    @Test
    @DisplayName("Test refresh returns auth response")
    void testRefresh() {
        RefreshTokenRequest req = new RefreshTokenRequest("token");
        when(authService.refreshToken(any())).thenReturn(authResponse);

        var result = authController.refreshToken(req);

        assertNotNull(result);
        verify(authService).refreshToken(any());
    }

    @Test
    @DisplayName("Test logout calls service")
    void testLogout() {
        RefreshTokenRequest req = new RefreshTokenRequest("token");

        authController.logout(req);

        verify(authService).logout(any());
    }
}
