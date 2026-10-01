package com.skillforge.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecretKey jwtSecretKey;

    @Value("${security.jwt.expiration}")
    private long expiration;

    public String generateToken(Long userId, String email) {

        Date issuedAt = new Date();
        Date expirationDate =
                new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(issuedAt)
                .expiration(expirationDate)
                .signWith(jwtSecretKey)
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

    public boolean isTokenValid(String token) {

        try {
            Claims claims = extractClaims(token);

            return claims.getExpiration().after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }
}