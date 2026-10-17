package com.skillforge.authservice.client;

public record UserProfileProvisionRequest(
        String username,
        String displayName,
        String bio,
        String avatarUrl
) {
}
