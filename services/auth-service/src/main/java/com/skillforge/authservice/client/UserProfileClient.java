package com.skillforge.authservice.client;

import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.exception.ProfileProvisioningException;
import com.skillforge.authservice.exception.ProfileConflictException;
import com.skillforge.authservice.security.JwtService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;

@Component
public class UserProfileClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public UserProfileClient(
            RestClient.Builder builder,
            JwtService jwtService
    ) {
        this.restClient = builder
                .baseUrl("http://USER-SERVICE")
                .build();
        this.jwtService = jwtService;
    }

    public void createProfile(Long userId, String email, RegisterRequest request) {
        String internalToken = jwtService.generateToken(userId, email);
        UserProfileProvisionRequest profile = new UserProfileProvisionRequest(
                request.getUsername(),
                request.getDisplayName(),
                request.getBio(),
                request.getAvatarUrl()
        );

        try {
            restClient.put()
                    .uri("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + internalToken)
                    .body(profile)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw new ProfileConflictException(
                        "The requested username is already taken",
                        exception
                );
            }
            throw new ProfileProvisioningException(
                    "Could not create the user profile",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ProfileProvisioningException(
                    "Could not create the user profile",
                    exception
            );
        }
    }

    public void deleteProfile(AuthUser user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        try {
            restClient.delete()
                    .uri("/api/users/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return;
            }
            throw new ProfileProvisioningException(
                    "Could not delete the user profile during compensation",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ProfileProvisioningException(
                    "Could not delete the user profile during compensation",
                    exception
            );
        }
    }
}
