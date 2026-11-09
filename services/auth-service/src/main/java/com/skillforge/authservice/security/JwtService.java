package com.skillforge.authservice.security;

import com.skillforge.authservice.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey jwtSecretKey;

    @Value("${security.jwt.expiration}")
    private long expiration;

    public JwtService( @Qualifier("userJwtSecretKey")
                       SecretKey jwtSecretKey){
        this.jwtSecretKey = jwtSecretKey;
    }

    public long getExpirationInSeconds() {
        return expiration / 1000;
    }

    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, null);
    }

    public String generateToken(Long userId, String email, UserRole role) {

        Date issuedAt = new Date();
        Date expirationDate =
                new Date(issuedAt.getTime() + expiration);

        var tokenBuilder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(issuedAt)
                .expiration(expirationDate);

        if (role != null) {
            tokenBuilder.claim("role", role.name());
        }

        return tokenBuilder
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {

        return Long.valueOf(
                extractClaims(token).getSubject()
        );
    }

    public Optional<UserRole> extractRole(String token) {
        return Optional.ofNullable(
                        extractClaims(token).get("role", String.class)
                )
                .map(UserRole::valueOf);
    }

    public boolean isTokenValid(String token) {

        try {
            Claims claims = extractClaims(token);

            String role = claims.get("role", String.class);
            if (role != null) {
                UserRole.valueOf(role);
            }

            return claims.getExpiration().after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }
}
