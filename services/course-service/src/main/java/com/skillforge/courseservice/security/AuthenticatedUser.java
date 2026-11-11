package com.skillforge.courseservice.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}
