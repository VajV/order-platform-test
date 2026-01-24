package com.ecommerce.notification.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Principal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewayUserPrincipal implements Principal {

    private String userId;
    private String email;
    private String roles;

    @Override
    public String getName() {
        return userId;
    }
}
