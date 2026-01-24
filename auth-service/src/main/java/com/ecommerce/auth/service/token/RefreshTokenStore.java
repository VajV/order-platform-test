package com.ecommerce.auth.service.token;

import java.util.Optional;

public interface RefreshTokenStore {

    void issue(Long userId, String refreshToken);

    Optional<Long> getUserId(String refreshToken);

    void revoke(String refreshToken);
}
