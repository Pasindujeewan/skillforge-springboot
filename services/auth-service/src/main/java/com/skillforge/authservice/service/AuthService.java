package com.skillforge.authservice.service;

import com.skillforge.authservice.dto.AuthResponse;
import com.skillforge.authservice.dto.LoginRequest;
import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.dto.RegistrationResponse;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.repository.AuthUserRepository;
import com.skillforge.authservice.security.JwtService;
import com.skillforge.authservice.saga.RegistrationSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.skillforge.authservice.exception.InvalidCredentialsException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RegistrationSagaOrchestrator registrationSagaOrchestrator;

    public RegistrationResponse register(RegisterRequest request) {
        return registrationSagaOrchestrator.register(request);
    }

    public AuthResponse login(LoginRequest request) {

        AuthUser user = authUserRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        return new AuthResponse(
                "Login successful",
                user.getId(),
                user.getEmail(),
                "Bearer",
                jwtService.getExpirationInSeconds(),
                token
        );
    }
}
