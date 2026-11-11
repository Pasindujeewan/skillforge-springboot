package com.skillforge.authservice.service;

import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.exception.EmailAlreadyRegisteredException;
import com.skillforge.authservice.repository.AuthUserRepository;
import com.skillforge.authservice.saga.RegistrationSaga;
import com.skillforge.authservice.saga.RegistrationSagaContext;
import com.skillforge.authservice.saga.RegistrationSagaRepository;
import com.skillforge.authservice.saga.RegistrationSagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthUserCommandService {

    private final AuthUserRepository authUserRepository;
    private final RegistrationSagaRepository sagaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistrationSagaContext createAuthUser(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }

        RegistrationSaga saga = sagaRepository.save(RegistrationSaga.start());
        AuthUser user = new AuthUser();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        try {
            AuthUser savedUser = authUserRepository.saveAndFlush(user);
            saga.authUserCreated(savedUser.getId());
            sagaRepository.save(saga);
            return new RegistrationSagaContext(
                    saga.getId(),
                    savedUser.getId(),
                    savedUser.getEmail()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered",
                    exception
            );
        }
    }

    @Transactional
    public void markProfileCreated(UUID sagaId) {
        findSaga(sagaId).profileCreated();
    }

    @Transactional
    public void complete(UUID sagaId) {
        findSaga(sagaId).complete();
    }

    @Transactional
    public Optional<AuthUser> beginCompensation(UUID sagaId) {
        RegistrationSaga saga = sagaRepository.findByIdForUpdate(sagaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registration saga not found: " + sagaId
                ));

        if (saga.getStatus() == RegistrationSagaStatus.COMPENSATED) {
            return Optional.empty();
        }

        if (saga.getStatus() != RegistrationSagaStatus.AUTH_USER_CREATED
                && saga.getStatus() != RegistrationSagaStatus.PROFILE_CREATED
                && saga.getStatus() != RegistrationSagaStatus.COMPENSATING
                && saga.getStatus() != RegistrationSagaStatus.COMPENSATION_FAILED) {
            throw new IllegalStateException(
                    "Registration saga cannot be compensated from status "
                            + saga.getStatus()
            );
        }

        saga.beginCompensation();
        return Optional.of(authUserRepository.findById(saga.getAuthUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Auth user not found for registration saga " + sagaId
                )));
    }

    @Transactional
    public void completeCompensation(UUID sagaId, Long authUserId) {
        RegistrationSaga saga = sagaRepository.findByIdForUpdate(sagaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registration saga not found: " + sagaId
                ));

        if (saga.getStatus() != RegistrationSagaStatus.COMPENSATING) {
            throw new IllegalStateException("Registration saga is not compensating");
        }

        saga.clearAuthUserReference();
        sagaRepository.saveAndFlush(saga);
        authUserRepository.deleteById(authUserId);
        authUserRepository.flush();
        saga.compensated();
    }

    @Transactional
    public void markCompensationFailed(UUID sagaId, Throwable failure) {
        sagaRepository.findByIdForUpdate(sagaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registration saga not found: " + sagaId
                ))
                .compensationFailed(failure);
    }

    private RegistrationSaga findSaga(UUID sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registration saga not found: " + sagaId
                ));
    }
}
