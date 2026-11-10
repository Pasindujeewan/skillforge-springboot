package com.skillforge.authservice.client;

import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.exception.UserServiceCircuitBreakerOpenException;
import com.skillforge.authservice.security.JwtService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileClientTest {

    private JwtService jwtService;
    private CircuitBreaker circuitBreaker;
    private UserProfileClient userProfileClient;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        circuitBreaker = CircuitBreaker.of(
                "user-service",
                CircuitBreakerConfig.ofDefaults()
        );
        circuitBreaker.transitionToOpenState();
        userProfileClient = new UserProfileClient(
                RestClient.builder(),
                jwtService,
                circuitBreaker
        );
        when(jwtService.generateToken(42L, "user@example.com")).thenReturn("internal-token");
    }

    @Test
    void createProfileFailsFastWithDedicatedExceptionWhenCircuitIsOpen() {
        UserServiceCircuitBreakerOpenException exception = assertThrows(
                UserServiceCircuitBreakerOpenException.class,
                () -> userProfileClient.createProfile(
                        42L,
                        "user@example.com",
                        new RegisterRequest()
                )
        );

        assertEquals(
                "User service circuit breaker is open; cannot create the user profile",
                exception.getMessage()
        );
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        verify(jwtService).generateToken(42L, "user@example.com");
    }
}
