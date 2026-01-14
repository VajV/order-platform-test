package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    @NotBlank(message = "Email обязателен")
    @Email(message = "Email должен быть валиден")
    private String email;

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 255)
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 255)
    private String lastName;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен быть минимум 8 символов")
    private String password;

    private JsonNode metadata;
}
