package com.skillforge.authservice.dto;

public record RegistrationResponse(
        String message,
        Long id,
        String email
) {
}
