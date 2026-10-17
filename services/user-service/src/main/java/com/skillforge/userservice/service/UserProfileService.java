package com.skillforge.userservice.service;


import com.skillforge.userservice.dto.ProfileRequest;
import com.skillforge.userservice.dto.ProfileResponse;
import com.skillforge.userservice.entity.UserProfile;
import com.skillforge.userservice.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long id) {
        UserProfile profile = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(
            Long id,
            ProfileRequest request
    ) {
        UserProfile profile = repository.findById(id)
                .orElse(null);

        boolean sameUsername = profile != null
                && profile.getUsername().equals(request.username());

        if (!sameUsername
                && repository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken"
            );
        }

        if (profile == null) {
            profile = new UserProfile(
                    id,
                    request.username(),
                    request.displayName(),
                    request.bio(),
                    request.avatarUrl()
            );
        } else {
            profile.update(
                    request.username(),
                    request.displayName(),
                    request.bio(),
                    request.avatarUrl()
            );
        }

        try {
            return ProfileResponse.from(repository.saveAndFlush(profile));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken",
                    exception
            );
        }
    }
}
