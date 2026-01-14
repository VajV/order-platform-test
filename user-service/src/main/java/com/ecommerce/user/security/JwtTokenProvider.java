package com.ecommerce.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {
    @Value("${jwt.secret:mysecretkeythatisatleast256bitslongandsecureforjwt12345}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationInMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())  // ← УБРАЛ SignatureAlgorithm
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()  // ← ИЗМЕНЕНО с parserBuilder()
                .verifyWith(getSigningKey())  // ← ИЗМЕНЕНО с setSigningKey()
                .build()
                .parseSignedClaims(token)  // ← ИЗМЕНЕНО с parseClaimsJws()
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()  // ← ИЗМЕНЕНО
                    .verifyWith(getSigningKey())  // ← ИЗМЕНЕНО
                    .build()
                    .parseSignedClaims(token);  // ← ИЗМЕНЕНО
            return true;
        } catch (JwtException ex) {  // ← Обобщенное исключение
            log.error("JWT validation error: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public String getRolesFromToken(String token) {
        return Jwts.parser()  // ← ИЗМЕНЕНО
                .verifyWith(getSigningKey())  // ← ИЗМЕНЕНО
                .build()
                .parseSignedClaims(token)  // ← ИЗМЕНЕНО
                .getPayload()
                .get("roles", String.class);
    }
}
