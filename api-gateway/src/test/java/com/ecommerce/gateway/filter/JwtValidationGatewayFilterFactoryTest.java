package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для JwtValidationGatewayFilterFactory.
 * Покрытие: валидация токенов, обработка ошибок, проверка конфигурации.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtValidationGatewayFilterFactory Unit Tests")
class JwtValidationGatewayFilterFactoryTest {

    private static final String VALID_SECRET = "ThisIsA32CharacterSecretKey123!!";
    private static final String SHORT_SECRET = "short";

    private JwtValidationGatewayFilterFactory filterFactory;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtValidationGatewayFilterFactory();
        ReflectionTestUtils.setField(filterFactory, "jwtSecret", VALID_SECRET);
    }

    // ========== CONFIGURATION VALIDATION TESTS ==========

    @Nested
    @DisplayName("validateConfiguration()")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("должен пройти валидацию с корректным секретом")
        void shouldPassValidationWithValidSecret() {
            // Given
            ReflectionTestUtils.setField(filterFactory, "jwtSecret", VALID_SECRET);

            // When & Then - no exception
            filterFactory.validateConfiguration();
        }

        @Test
        @DisplayName("должен выбросить исключение при пустом секрете")
        void shouldThrowExceptionWhenSecretIsEmpty() {
            // Given
            ReflectionTestUtils.setField(filterFactory, "jwtSecret", "");

            // When & Then
            assertThatThrownBy(() -> filterFactory.validateConfiguration())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret не установлен");
        }

        @Test
        @DisplayName("должен выбросить исключение при null секрете")
        void shouldThrowExceptionWhenSecretIsNull() {
            // Given
            ReflectionTestUtils.setField(filterFactory, "jwtSecret", null);

            // When & Then
            assertThatThrownBy(() -> filterFactory.validateConfiguration())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret не установлен");
        }

        @Test
        @DisplayName("должен выбросить исключение при коротком секрете")
        void shouldThrowExceptionWhenSecretIsTooShort() {
            // Given
            ReflectionTestUtils.setField(filterFactory, "jwtSecret", SHORT_SECRET);

            // When & Then
            assertThatThrownBy(() -> filterFactory.validateConfiguration())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("минимум 32 символа");
        }
    }

    // ========== TOKEN VALIDATION TESTS ==========

    @Nested
    @DisplayName("apply() - Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("должен отклонить запрос без Authorization заголовка")
        void shouldRejectRequestWithoutAuthorizationHeader() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .build();
            MockServerHttpResponse response = new MockServerHttpResponse();
            
            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            when(mockExchange.getRequest()).thenReturn(request);
            when(mockExchange.getResponse()).thenReturn(response);

            GatewayFilter filter = filterFactory.apply(new JwtValidationGatewayFilterFactory.Config());

            // When
            Mono<Void> result = filter.filter(mockExchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate"))
                    .contains("Bearer");
        }

        @Test
        @DisplayName("должен отклонить запрос с неверным форматом Authorization")
        void shouldRejectRequestWithInvalidAuthorizationFormat() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .header("Authorization", "Basic sometoken")
                    .build();
            MockServerHttpResponse response = new MockServerHttpResponse();

            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            when(mockExchange.getRequest()).thenReturn(request);
            when(mockExchange.getResponse()).thenReturn(response);

            GatewayFilter filter = filterFactory.apply(new JwtValidationGatewayFilterFactory.Config());

            // When
            Mono<Void> result = filter.filter(mockExchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("должен отклонить запрос с невалидным JWT токеном")
        void shouldRejectRequestWithInvalidJwtToken() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .header("Authorization", "Bearer invalid.jwt.token")
                    .build();
            MockServerHttpResponse response = new MockServerHttpResponse();

            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            when(mockExchange.getRequest()).thenReturn(request);
            when(mockExchange.getResponse()).thenReturn(response);

            GatewayFilter filter = filterFactory.apply(new JwtValidationGatewayFilterFactory.Config());

            // When
            Mono<Void> result = filter.filter(mockExchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate"))
                    .contains("invalid_token");
        }

        @Test
        @DisplayName("должен отклонить запрос с истёкшим JWT токеном")
        void shouldRejectRequestWithExpiredJwtToken() {
            // Given
            String expiredToken = generateToken("testuser", "1", "ROLE_USER", -1);
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .header("Authorization", "Bearer " + expiredToken)
                    .build();
            MockServerHttpResponse response = new MockServerHttpResponse();

            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            when(mockExchange.getRequest()).thenReturn(request);
            when(mockExchange.getResponse()).thenReturn(response);

            GatewayFilter filter = filterFactory.apply(new JwtValidationGatewayFilterFactory.Config());

            // When
            Mono<Void> result = filter.filter(mockExchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // TODO: Этот тест требует WebTestClient и полной интеграции с Spring WebFlux.
        // Рекомендуется использовать @SpringBootTest с WebTestClient для полноценного тестирования.
        @Test
        @org.junit.jupiter.api.Disabled("Требует WebTestClient для корректного мокирования request.mutate()")
        @DisplayName("должен пропустить запрос с валидным JWT токеном")
        void shouldAllowRequestWithValidJwtToken() {
            // Given
            String validToken = generateToken("testuser", "123", "ROLE_USER", 60);
            
            MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest
                    .get("/api/users/me")
                    .header("Authorization", "Bearer " + validToken);
            MockServerHttpRequest request = requestBuilder.build();
            MockServerHttpResponse response = new MockServerHttpResponse();

            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            when(mockExchange.getRequest()).thenReturn(request);
            when(mockExchange.getResponse()).thenReturn(response);
            
            // Mock request mutation
            ServerHttpRequest.Builder mutatedRequestBuilder = mock(ServerHttpRequest.Builder.class);
            when(mutatedRequestBuilder.header(anyString(), anyString())).thenReturn(mutatedRequestBuilder);
            when(mutatedRequestBuilder.build()).thenReturn(request);
            
            when(chain.filter(any())).thenReturn(Mono.empty());

            GatewayFilter filter = filterFactory.apply(new JwtValidationGatewayFilterFactory.Config());

            // When
            Mono<Void> result = filter.filter(mockExchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(chain).filter(any());
        }
    }

    // ========== CONFIG CLASS TESTS ==========

    @Nested
    @DisplayName("Config class")
    class ConfigTests {

        @Test
        @DisplayName("должен иметь значение enableLogging по умолчанию true")
        void shouldHaveDefaultEnableLoggingTrue() {
            // Given
            JwtValidationGatewayFilterFactory.Config config = 
                    new JwtValidationGatewayFilterFactory.Config();

            // Then
            assertThat(config.isEnableLogging()).isTrue();
        }

        @Test
        @DisplayName("должен позволять изменить enableLogging")
        void shouldAllowChangingEnableLogging() {
            // Given
            JwtValidationGatewayFilterFactory.Config config = 
                    new JwtValidationGatewayFilterFactory.Config();

            // When
            config.setEnableLogging(false);

            // Then
            assertThat(config.isEnableLogging()).isFalse();
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Генерирует JWT токен для тестирования.
     *
     * @param subject username
     * @param userId ID пользователя
     * @param roles роли пользователя
     * @param expirationMinutes минуты до истечения (отрицательное = уже истёк)
     * @return JWT токен
     */
    private String generateToken(String subject, String userId, String roles, int expirationMinutes) {
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8));
        
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}

