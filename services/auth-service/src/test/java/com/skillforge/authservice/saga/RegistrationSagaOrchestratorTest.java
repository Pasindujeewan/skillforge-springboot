package com.skillforge.authservice.saga;

import com.skillforge.authservice.client.UserProfileClient;
import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.dto.RegistrationResponse;
import com.skillforge.authservice.exception.ProfileConflictException;
import com.skillforge.authservice.exception.RegistrationSagaException;
import com.skillforge.authservice.service.AuthUserCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationSagaOrchestratorTest {

    @Mock
    private AuthUserCommandService authUserCommandService;

    @Mock
    private UserProfileClient userProfileClient;

    @Mock
    private RegistrationSagaCompensationService compensationService;

    @InjectMocks
    private RegistrationSagaOrchestrator orchestrator;

    private RegisterRequest request;
    private RegistrationSagaContext context;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("strong-password");
        context = new RegistrationSagaContext(
                UUID.randomUUID(),
                42L,
                "user@example.com"
        );
        when(authUserCommandService.createAuthUser(request)).thenReturn(context);
    }

    @Test
    void registerCreatesProfileAndCompletesSaga() {
        RegistrationResponse response = orchestrator.register(request);

        assertEquals(42L, response.id());
        assertEquals("user@example.com", response.email());
        verify(userProfileClient).createProfile(42L, "user@example.com", request);
        verify(authUserCommandService).markProfileCreated(context.sagaId());
        verify(authUserCommandService).complete(context.sagaId());
    }

    @Test
    void registerCompensatesWhenProfileCreationFails() {
        ProfileConflictException failure =
                new ProfileConflictException("Username is taken", null);
        doThrow(failure)
                .when(userProfileClient)
                .createProfile(42L, "user@example.com", request);
        when(compensationService.compensate(context.sagaId())).thenReturn(true);

        ProfileConflictException thrown = assertThrows(
                ProfileConflictException.class,
                () -> orchestrator.register(request)
        );

        assertEquals(failure, thrown);
        verify(compensationService).compensate(context.sagaId());
    }

    @Test
    void registerReportsPendingCleanupWhenCompensationFails() {
        doThrow(new IllegalStateException("Profile service unavailable"))
                .when(userProfileClient)
                .createProfile(42L, "user@example.com", request);
        when(compensationService.compensate(context.sagaId())).thenReturn(false);

        assertThrows(
                RegistrationSagaException.class,
                () -> orchestrator.register(request)
        );
        verify(compensationService).compensate(context.sagaId());
    }
}
