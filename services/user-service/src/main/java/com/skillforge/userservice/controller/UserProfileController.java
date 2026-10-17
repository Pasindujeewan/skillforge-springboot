package com.skillforge.userservice.controller;


import com.skillforge.userservice.dto.ProfileRequest;
import com.skillforge.userservice.dto.ProfileResponse;
import com.skillforge.userservice.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile(
            JwtAuthenticationToken authentication
    ) {
        UUID userId = profileIdFor(authentication.getToken().getSubject());

        return profileService.getProfile(userId);
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody ProfileRequest request
    ) {
        UUID userId = profileIdFor(authentication.getToken().getSubject());

        return profileService.updateProfile(userId, request);
    }

    @GetMapping("/public/{id}")
    public ProfileResponse getPublicProfile(
            @PathVariable UUID id
    ) {
        return profileService.getProfile(id);
    }

    private UUID profileIdFor(String authUserId) {
        return UUID.nameUUIDFromBytes(
                ("skillforge-user:" + authUserId).getBytes(StandardCharsets.UTF_8)
        );
    }
}
