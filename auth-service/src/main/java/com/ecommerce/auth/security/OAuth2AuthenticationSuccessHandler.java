package com.ecommerce.auth.security;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import com.ecommerce.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Обработчик успешной OAuth2 аутентификации.
 * Создаёт или находит пользователя, генерирует JWT и перенаправляет на frontend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            
            log.info("OAuth2 authentication success for provider: {}", registrationId);
            
            // Извлечь данные пользователя из OAuth2 провайдера
            UserInfo userInfo = extractUserInfo(oAuth2User, registrationId);
            
            // Найти или создать пользователя
            User user = findOrCreateUser(userInfo, registrationId);
            
            // Генерировать JWT токены
            String accessToken = jwtUtil.generateToken(user.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
            
            log.info("Generated JWT for OAuth2 user: {}", user.getEmail());
            
            // Перенаправить с токенами
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", registrationId)
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private UserInfo extractUserInfo(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        return switch (registrationId.toLowerCase()) {
            case "google" -> new UserInfo(
                    (String) attributes.get("email"),
                    (String) attributes.get("given_name"),
                    (String) attributes.get("family_name"),
                    (String) attributes.get("sub"),
                    registrationId
            );
            case "github" -> {
                String email = (String) attributes.get("email");
                if (email == null) {
                    // GitHub может не вернуть email, используем login
                    email = attributes.get("login") + "@github.com";
                }
                String name = (String) attributes.get("name");
                String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
                yield new UserInfo(
                        email,
                        nameParts[0],
                        nameParts.length > 1 ? nameParts[1] : "",
                        String.valueOf(attributes.get("id")),
                        registrationId
                );
            }
            default -> throw new IllegalArgumentException("Unknown OAuth2 provider: " + registrationId);
        };
    }

    private User findOrCreateUser(UserInfo userInfo, String provider) {
        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Обновить OAuth провайдер если нужно
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthId(userInfo.oauthId());
                userRepository.save(user);
            }
            return user;
        }
        
        // Создать нового пользователя
        User newUser = new User();
        newUser.setEmail(userInfo.email());
        newUser.setFirstName(userInfo.firstName());
        newUser.setLastName(userInfo.lastName());
        newUser.setOauthProvider(provider);
        newUser.setOauthId(userInfo.oauthId());
        newUser.setEnabled(true);
        newUser.setRole(UserRole.ROLE_USER);
        // Для OAuth пользователей пароль не нужен, но поле не null
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");
        
        User savedUser = userRepository.save(newUser);
        log.info("Created new OAuth2 user: {} via {}", savedUser.getEmail(), provider);
        
        return savedUser;
    }

    private record UserInfo(
            String email,
            String firstName,
            String lastName,
            String oauthId,
            String provider
    ) {}
}

