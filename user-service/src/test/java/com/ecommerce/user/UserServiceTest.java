package com.ecommerce.user;

import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для UserService.
 * Покрытие: регистрация, CRUD операции, управление ролями.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private UserUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ROLE_ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setRoles(new HashSet<>(Set.of(userRole)));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setDeletedAt(null);

        updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
    }

    @Nested
    @DisplayName("handleUserCreatedEvent()")
    class HandleUserCreatedEventTests {

        @Test
        @DisplayName("должен создать профиль пользователя из user.created и назначить ROLE_USER")
        void shouldCreateProfileFromUserCreated() {
            when(userRepository.existsById(100L)).thenReturn(false);
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.handleUserCreatedEvent(100L, "john", "john@example.com", Instant.now());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("должен быть идемпотентным (не создавать повторно если userId уже есть)")
        void shouldBeIdempotent() {
            when(userRepository.existsById(100L)).thenReturn(true);

            userService.handleUserCreatedEvent(100L, "john", "john@example.com", Instant.now());

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ========== GET USER TESTS ==========

    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("должен вернуть пользователя по ID")
        void shouldReturnUserById() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserResponse response = userService.getUserById(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("должен выбросить UserNotFoundException если пользователь не найден")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }
    }

    // ========== GET USER BY EMAIL TESTS ==========

    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmailTests {

        @Test
        @DisplayName("должен вернуть пользователя по email")
        void shouldReturnUserByEmail() {
            // Given
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserResponse response = userService.getUserByEmail("test@example.com");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("должен выбросить UserNotFoundException если email не найден")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Given
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with email");
        }
    }

    // ========== GET ALL USERS TESTS ==========

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsersTests {

        @Test
        @DisplayName("должен вернуть страницу пользователей")
        void shouldReturnPageOfUsers() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
            when(userRepository.findActiveUsers(pageable)).thenReturn(userPage);

            // When
            Page<UserResponse> response = userService.getAllUsers(pageable);

            // Then
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        }
    }

    // ========== UPDATE USER TESTS ==========

    @Nested
    @DisplayName("updateUser()")
    class UpdateUserTests {

        @Test
        @DisplayName("должен обновить имя пользователя")
        void shouldUpdateUserName() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserResponse response = userService.updateUser(1L, updateRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(testUser.getFirstName()).isEqualTo("Updated");
            assertThat(testUser.getLastName()).isEqualTo("Name");
        }

        @Test
        @DisplayName("должен обновить email если он уникален")
        void shouldUpdateEmailIfUnique() {
            // Given
            updateRequest.setEmail("newemail@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserResponse response = userService.updateUser(1L, updateRequest);

            // Then
            assertThat(testUser.getEmail()).isEqualTo("newemail@example.com");
        }

        @Test
        @DisplayName("должен выбросить исключение если новый email уже занят")
        void shouldThrowExceptionWhenNewEmailExists() {
            // Given
            updateRequest.setEmail("existing@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        @DisplayName("не должен проверять email если он не изменился")
        void shouldNotCheckEmailIfUnchanged() {
            // Given
            updateRequest.setEmail("test@example.com"); // Same as current
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.updateUser(1L, updateRequest);

            // Then
            verify(userRepository, never()).existsByEmail(anyString());
        }
    }

    // ========== DELETE USER TESTS ==========

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("должен деактивировать пользователя (soft delete)")
        void shouldDeactivateUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userService.deleteUser(1L);

            // Then
            assertThat(testUser.isActive()).isFalse();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("должен выбросить исключение если пользователь не найден")
        void shouldThrowExceptionWhenUserNotFoundOnDelete() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ========== UPDATE USER ROLES TESTS ==========

    @Nested
    @DisplayName("updateUserRoles()")
    class UpdateUserRolesTests {

        @Test
        @DisplayName("должен обновить роли пользователя")
        void shouldUpdateUserRoles() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserResponse response = userService.updateUserRoles(1L, List.of("ROLE_ADMIN"));

            // Then
            assertThat(testUser.getRoles()).contains(adminRole);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("должен выбросить исключение если роль не найдена")
        void shouldThrowExceptionWhenRoleNotFoundOnUpdate() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("ROLE_UNKNOWN")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateUserRoles(1L, List.of("ROLE_UNKNOWN")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    // ========== IS CURRENT USER TESTS ==========

    @Nested
    @DisplayName("isCurrentUser()")
    class IsCurrentUserTests {

        @Test
        @DisplayName("должен вернуть false если пользователь не найден или нет authentication")
        void shouldReturnFalseWhenUserNotFoundOrNoAuth() {
            // Given
            // Метод isCurrentUser() сначала вызывает getCurrentUserEmail(),
            // который бросит исключение если нет SecurityContext.
            // В этом случае метод возвращает false без вызова repository.

            // When
            boolean result = userService.isCurrentUser(999L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
