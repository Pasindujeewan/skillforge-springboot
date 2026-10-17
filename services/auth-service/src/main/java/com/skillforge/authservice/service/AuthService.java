package com.skillforge.authservice.service;

import com.skillforge.authservice.dto.AuthResponse;
import com.skillforge.authservice.dto.LoginRequest;
import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.repository.AuthUserRepository;
import com.skillforge.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.skillforge.authservice.exception.EmailAlreadyRegisteredException;
import com.skillforge.authservice.exception.InvalidCredentialsException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {

        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered"
            );
        }

        AuthUser user = new AuthUser();

        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        try {
            authUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered",
                    exception
            );
        }
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

        return new AuthResponse(user.getId(), token);
    }
}
