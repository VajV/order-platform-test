package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserDto;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.UserCreatedEvent;
import com.ecommerce.auth.exception.BadRequestException;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtUtil;
import com.ecommerce.auth.service.token.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenStore refreshTokenStore;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Регистрация нового пользователя
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        // Проверка существования пользователя
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists: " + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists: " + request.email());
        }

        // Создание пользователя
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(User.Role.ROLE_USER)
                .accountNonExpired(true)
                .enabled(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        eventPublisher.publishEvent(new UserCreatedEvent(user.getId(), user.getUsername(), user.getEmail()));

        // Генерация токенов
        return generateAuthResponse(user);
    }

    /**
     * Вход пользователя
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.username());

        // Аутентификация
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        // Обновление lastLoginAt
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getUsername());

        // Генерация токенов
        return generateAuthResponse(user);
    }

    /**
     * Обновление access token через refresh token
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request");

        // Валидация refresh token
        if (!jwtUtil.validateToken(request.refreshToken()) || !jwtUtil.isRefreshToken(request.refreshToken())) {
            throw new BadRequestException("Invalid refresh token");
        }

        Long userId = refreshTokenStore.getUserId(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Генерация нового access token
        String newAccessToken = jwtUtil.generateAccessToken(user);

        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );

        log.info("Access token refreshed for user: {}", user.getUsername());

        // ✅ ИСПРАВЛЕНО: используем new вместо builder()
        return new AuthResponse(
                newAccessToken,
                request.refreshToken(),
                "Bearer",
                900L, // 15 минут
                userDto
        );
    }

    /**
     * Выход пользователя (revoke refresh token)
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        log.info("Logout request");

        refreshTokenStore.revoke(refreshTokenValue);
    }

    /**
     * Генерация AuthResponse с токенами
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenValue = jwtUtil.generateRefreshToken(user);

        refreshTokenStore.issue(user.getId(), refreshTokenValue);

        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );

        // ✅ ИСПРАВЛЕНО: используем new вместо builder()
        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                900L, // 15 минут
                userDto
        );
    }
}
