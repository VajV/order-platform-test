package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolesRequest {
    @NotEmpty(message = "Роли не должны быть пусты")
    private Set<String> roleNames;
}
