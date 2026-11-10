package com.skillforge.authservice.client;

import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.exception.ProfileProvisioningException;
import com.skillforge.authservice.exception.ProfileConflictException;
import com.skillforge.authservice.exception.UserServiceCircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import com.skillforge.authservice.security.JwtService;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;

@Component
public class UserProfileClient {

    private final RestClient restClient;
    private final JwtService jwtService;
    private final CircuitBreaker circuitBreaker;

    public UserProfileClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            JwtService jwtService,
            CircuitBreaker userServiceCircuitBreaker
    ) {
        this.restClient = builder
                .baseUrl("http://USER-SERVICE")
                .build();
        this.jwtService = jwtService;
        this.circuitBreaker = userServiceCircuitBreaker;
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
            executeWithCircuitBreaker(() -> restClient.put()
                            .uri("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + internalToken)
                            .body(profile)
                            .retrieve()
                            .toBodilessEntity(),
                    "create the user profile");
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
            executeWithCircuitBreaker(() -> restClient.delete()
                            .uri("/api/users/me")
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .toBodilessEntity(),
                            "delete the user profile during compensation");
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

    private void executeWithCircuitBreaker(Runnable operation, String action) {
        try {
            circuitBreaker.executeRunnable(operation);
        } catch (CallNotPermittedException exception) {
            throw new UserServiceCircuitBreakerOpenException(
                    "User service circuit breaker is open; cannot " + action,
                    exception
            );
        }
    }
}
