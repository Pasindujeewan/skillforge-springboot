package com.skillforge.userservice.dto;

import com.skillforge.userservice.entity.UserProfile;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
