package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * User Entity - реализует Spring Security UserDetails
 * ✅ FIX: Полная реализация методов UserDetails
 * ✅ FIX: @CreationTimestamp и @UpdateTimestamp для автоматической инициализации дат
 * ✅ FIX: Role enum определен внутри класса
 * ✅ FIX: @Builder.Default для boolean полей
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Builder.Default  // ✅ ДОБАВЛЕНО!
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default  // ✅ ДОБАВЛЕНО!
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default  // ✅ ДОБАВЛЕНО!
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default  // ✅ ДОБАВЛЕНО!
    private boolean credentialsNonExpired = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ============================================================
    // UserDetails Implementation Methods
    // ============================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(role.name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ============================================================
    // Role Enum
    // ============================================================

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_MANAGER
    }

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Factory method для создания нового пользователя при регистрации
     */
    public static User createNewUser(String username, String email, String password, String fullName) {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .fullName(fullName)
                .role(Role.ROLE_USER)
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .build();
    }
}
