package com.skillforge.authservice.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class InternalTokenService {

    private final SecretKey internalJwtSecretKey;

    @Value("${security.internal-jwt.expiration}")
    private long expiration;

    public InternalTokenService(
            @Qualifier("internalJwtSecretKey")
            SecretKey internalJwtSecretKey
    ) {
        this.internalJwtSecretKey = internalJwtSecretKey;
    }

    public String generateToken() {

        Date issuedAt = new Date();
        Date expirationDate =
                new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .subject("auth-service")
                .issuer("auth-service")
                .audience()
                .add("user-service")
                .and()
                .claim("tokenType", "internal")
                .issuedAt(issuedAt)
                .expiration(expirationDate)
                .signWith(internalJwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }
}