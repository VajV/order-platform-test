package com.ecommerce.product.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${spring.security.jwt.secret:your-256-bit-secret-key-change-this-in-production}")
    private String jwtSecret;

    @Value("${spring.security.jwt.expiration:900000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromJwt(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String username = claims.getSubject();
            log.debug("Extracted username from JWT: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Error extracting username from JWT: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromJwt(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object userIdObj = claims.get("userId");

            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }

            log.warn("userId not found in JWT or has unexpected type");
            return null;
        } catch (Exception e) {
            log.error("Error extracting userId from JWT: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromJwt(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("Error extracting email from JWT: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromJwt(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object rolesObj = claims.get("roles");

            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }

            log.warn("roles not found in JWT or has unexpected type");
            return List.of();
        } catch (Exception e) {
            log.error("Error extracting roles from JWT: {}", e.getMessage());
            return List.of();
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            boolean expired = expiration.before(new Date());
            log.debug("Token expired: {}", expired);
            return expired;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            boolean valid = !isTokenExpired(token);
            log.debug("Token validation result: {}", valid);
            return valid;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    public <T> T getClaimFromToken(String token, String claimKey, Class<T> claimType) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get(claimKey, claimType);
        } catch (Exception e) {
            log.error("Error extracting claim '{}' from JWT: {}", claimKey, e.getMessage());
            return null;
        }
    }
}
