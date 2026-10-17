package com.skillforge.authservice.dto;

public record AuthResponse(
        String message,
        Long id,
        String email,
        String tokenType,
        long expiresIn,
        String accessToken
) {
}
