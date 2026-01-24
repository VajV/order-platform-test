package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.UserCreatedEvent;
import com.ecommerce.auth.exception.BadRequestException;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtUtil;
import com.ecommerce.auth.service.token.RefreshTokenStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "newuser",
                "newuser@example.com",
                "SecurePassword123",
                "New User"
        );

        loginRequest = new LoginRequest("testuser", "SecurePassword123");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .fullName("Test User")
                .role(User.Role.ROLE_USER)
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("register: успешная регистрация")
    void register_WithValidRequest_Success() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotEmpty();
        assertThat(response.refreshToken()).isNotEmpty();
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(userRepository).save(any(User.class));
        verify(refreshTokenStore).issue(testUser.getId(), "refresh_token");
        verify(eventPublisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    @DisplayName("register: ошибка - username существует")
    void register_WithExistingUsername_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: ошибка - email существует")
    void register_WithExistingEmail_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login: успешный вход")
    void login_WithValidCredentials_Success() {
        var mockAuth = new UsernamePasswordAuthenticationToken(
                testUser,
                testUser.getPassword(),
                testUser.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotEmpty();

        verify(userRepository).save(any(User.class));
        verify(refreshTokenStore).issue(testUser.getId(), "refresh_token");
    }

    @Test
    @DisplayName("login: ошибка - неверные credentials")
    void login_WithInvalidCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("refreshToken: успешное обновление")
    void refreshToken_WithValidToken_Success() {
        var request = new RefreshTokenRequest("valid_refresh_token");

        when(jwtUtil.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("valid_refresh_token")).thenReturn(true);
        when(refreshTokenStore.getUserId("valid_refresh_token")).thenReturn(Optional.of(testUser.getId()));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("new_access_token");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new_access_token");
    }

    @Test
    @DisplayName("refreshToken: ошибка - невалидный token")
    void refreshToken_WithInvalidToken_ThrowsException() {
        var request = new RefreshTokenRequest("invalid_token");

        when(jwtUtil.validateToken("invalid_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("logout: успешный logout")
    void logout_WithValidToken_Success() {
        String refreshTokenValue = "valid_refresh_token";

        authService.logout(refreshTokenValue);

        verify(refreshTokenStore).revoke(refreshTokenValue);
    }
}
