package com.skillforge.authservice.service;

import com.skillforge.authservice.repository.AuthUserRepository;
import com.skillforge.authservice.saga.RegistrationSaga;
import com.skillforge.authservice.saga.RegistrationSagaRepository;
import com.skillforge.authservice.saga.RegistrationSagaStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserCommandServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private RegistrationSagaRepository sagaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthUserCommandService authUserCommandService;

    @Test
    void completeCompensationClearsSagaReferenceBeforeDeletingAuthUser() {
        UUID sagaId = UUID.randomUUID();
        Long authUserId = 42L;
        RegistrationSaga saga = RegistrationSaga.start();
        saga.authUserCreated(authUserId);
        saga.beginCompensation();
        when(sagaRepository.findByIdForUpdate(sagaId)).thenReturn(Optional.of(saga));

        authUserCommandService.completeCompensation(sagaId, authUserId);

        InOrder order = inOrder(sagaRepository, authUserRepository);
        order.verify(sagaRepository).saveAndFlush(saga);
        order.verify(authUserRepository).deleteById(authUserId);
        order.verify(authUserRepository).flush();
        assertEquals(RegistrationSagaStatus.COMPENSATED, saga.getStatus());
        assertNull(saga.getAuthUserId());
        verify(sagaRepository).findByIdForUpdate(sagaId);
    }
}
