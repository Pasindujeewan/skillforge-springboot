package com.skillforge.authservice.security;

import com.skillforge.authservice.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecretKey secretKey = Keys.secretKeyFor(Jwts.SIG.HS256);
        jwtService = new JwtService(secretKey);
        ReflectionTestUtils.setField(jwtService, "expiration", 900_000L);
    }

    @Test
    void generateTokenIncludesRoleClaim() {
        String token = jwtService.generateToken(
                42L,
                "alex@example.com",
                UserRole.INSTRUCTOR
        );

        assertEquals(
                "INSTRUCTOR",
                jwtService.extractClaims(token).get("role", String.class)
        );
        assertEquals(Optional.of(UserRole.INSTRUCTOR), jwtService.extractRole(token));
    }

    @Test
    void tokenWithoutRoleHasNoRoleAuthorityForBackwardCompatibility() {
        String token = jwtService.generateToken(42L, "alex@example.com");

        assertTrue(jwtService.extractRole(token).isEmpty());
        assertTrue(jwtService.isTokenValid(token));
    }
}
