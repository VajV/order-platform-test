package com.ecommerce.user.service;

import com.ecommerce.user.dto.UpdateRolesRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public void handleUserCreatedEvent(Long userId, String username, String email, Instant timestamp) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        if (userRepository.existsById(userId)) {
            log.info("User already exists for userId={}, skipping", userId);
            return;
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));

        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(true);
        user.setDeletedAt(null);
        user.setCreatedAt(timestamp != null ? LocalDateTime.ofInstant(timestamp, java.time.ZoneOffset.UTC) : LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(userRole)));

        userRepository.save(user);
        log.info("Created profile user record for userId={} from user.created", userId);
    }

    private User requireActiveUser(Long userId, String notFoundMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(notFoundMessage));
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new UserNotFoundException(notFoundMessage);
        }
        return user;
    }

    // ✅ ДОБАВЛЕН метод getCurrentUser
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Long userId = getCurrentUserId();
        return getUserById(userId);
    }

    // ✅ ДОБАВЛЕН метод updateCurrentUser
    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        Long userId = getCurrentUserId();
        User user = requireActiveUser(userId, "Current user not found");

        // Обновить только разрешенные поля
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User updatedUser = userRepository.save(user);
        log.info("Current user updated successfully: {}", userId);
        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(Long userId) {
        User user = requireActiveUser(userId, "User not found with id: " + userId);
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String username, String email, String role, Pageable pageable) {
        return userRepository.searchActiveUsers(username, email, role, pageable)
                .map(this::mapToResponse);
    }

    // ✅ ДОБАВЛЕН метод updateRoles (вызывается из Controller)
    @Transactional
    public UserResponse updateRoles(Long userId, UpdateRolesRequest request) {
        return updateUserRoles(userId, request.getRoles());
    }

    // ✅ ДОБАВЛЕН метод для SpEL проверки (@PreAuthorize)
    public boolean isCurrentUser(Long userId) {
        try {
            Long currentUserId = getCurrentUserId();
            return currentUserId != null && currentUserId.equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        User user = requireActiveUser(id, "User not found with id: " + id);
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null && u.isActive())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Getting all users, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findActiveUsers(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user with id: {}", id);

        User user = requireActiveUser(id, "User not found with id: " + id);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
            }
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with id: {}", updatedUser.getId());
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = requireActiveUser(id, "User not found with id: " + id);

        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User deactivated successfully with id: {}", id);
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, List<String> roleNames) {
        log.info("Updating roles for user id: {}", userId);

        User user = requireActiveUser(userId, "User not found with id: " + userId);

        Set<Role> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        User updatedUser = userRepository.save(user);

        log.info("User roles updated successfully for user id: {}", userId);
        return mapToResponse(updatedUser);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user id in authentication context");
        }
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))  // ← List вместо Set
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .metadata(user.getMetadata())
                .build();
    }
}
