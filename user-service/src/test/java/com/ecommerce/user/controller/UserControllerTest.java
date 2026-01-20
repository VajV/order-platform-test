package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserCreateRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для UserController.
 * Использует @WebMvcTest для тестирования REST API.
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserResponse createTestUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .roles(List.of("ROLE_USER"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== REGISTER TESTS ==========

    @Nested
    @DisplayName("POST /api/users/register")
    class RegisterTests {

        @Test
        @DisplayName("должен зарегистрировать нового пользователя")
        void shouldRegisterNewUser() throws Exception {
            // Given
            UserCreateRequest request = new UserCreateRequest();
            request.setEmail("new@example.com");
            request.setPassword("SecurePassword123");
            request.setFirstName("New");
            request.setLastName("User");

            when(userService.registerUser(any(UserCreateRequest.class)))
                    .thenReturn(createTestUserResponse());

            // When & Then
            mockMvc.perform(post("/api/users/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.email").value("test@example.com"));

            verify(userService).registerUser(any(UserCreateRequest.class));
        }

        @Test
        @DisplayName("должен вернуть 409 если email уже существует")
        void shouldReturn409WhenEmailExists() throws Exception {
            // Given
            UserCreateRequest request = new UserCreateRequest();
            request.setEmail("existing@example.com");
            request.setPassword("Password123");
            request.setFirstName("Test");
            request.setLastName("User");

            when(userService.registerUser(any(UserCreateRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("User with email existing@example.com already exists"));

            // When & Then
            mockMvc.perform(post("/api/users/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("должен вернуть 400 при невалидном запросе")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given - пустой email
            UserCreateRequest request = new UserCreateRequest();
            request.setEmail("");
            request.setPassword("pass");

            // When & Then
            mockMvc.perform(post("/api/users/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== GET CURRENT USER TESTS ==========

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUserTests {

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("должен вернуть текущего пользователя")
        void shouldReturnCurrentUser() throws Exception {
            // Given
            when(userService.getCurrentUser()).thenReturn(createTestUserResponse());

            // When & Then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("должен вернуть 401 без аутентификации")
        void shouldReturn401WithoutAuth() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET ALL USERS TESTS ==========

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin должен получить список пользователей")
        void adminShouldGetAllUsers() throws Exception {
            // Given
            Page<UserResponse> page = new PageImpl<>(List.of(createTestUserResponse()));
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("обычный пользователь не должен получить список")
        void regularUserShouldNotGetAllUsers() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET USER BY ID TESTS ==========

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin должен получить пользователя по ID")
        void adminShouldGetUserById() throws Exception {
            // Given
            when(userService.getUserById(1L)).thenReturn(createTestUserResponse());

            // When & Then
            mockMvc.perform(get("/api/users/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("должен вернуть 404 если пользователь не найден")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            when(userService.getUserById(999L))
                    .thenThrow(new UserNotFoundException("User not found with id: 999"));

            // When & Then
            mockMvc.perform(get("/api/users/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}

