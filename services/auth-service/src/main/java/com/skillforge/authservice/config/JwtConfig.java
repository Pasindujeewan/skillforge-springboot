package com.skillforge.authservice.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.internal-jwt.secret}")
    private String internalJwtSecret;

    @Bean
    @Qualifier("userJwtSecretKey")
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    @Bean
    @Qualifier("internalJwtSecretKey")
    public SecretKey internalJwtSecretKey() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(internalJwtSecret)
        );
    }
}