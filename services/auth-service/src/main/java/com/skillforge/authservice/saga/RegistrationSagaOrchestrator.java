package com.skillforge.authservice.saga;

import com.skillforge.authservice.client.UserProfileClient;
import com.skillforge.authservice.dto.RegisterRequest;
import com.skillforge.authservice.dto.RegistrationResponse;
import com.skillforge.authservice.exception.RegistrationSagaException;
import com.skillforge.authservice.service.AuthUserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationSagaOrchestrator {

    private final AuthUserCommandService authUserCommandService;
    private final UserProfileClient userProfileClient;
    private final RegistrationSagaCompensationService compensationService;

    public RegistrationResponse register(RegisterRequest request) {
        RegistrationSagaContext context =
                authUserCommandService.createAuthUser(request);

        try {
            userProfileClient.createProfile(
                    context.authUserId(),
                    context.email(),
                    request
            );
            authUserCommandService.markProfileCreated(context.sagaId());
            authUserCommandService.complete(context.sagaId());
        } catch (RuntimeException failure) {
            if (!compensationService.compensate(context.sagaId())) {
                throw new RegistrationSagaException(
                        "Registration failed and compensation is pending",
                        failure
                );
            }
            throw failure;
        }

        return new RegistrationResponse(
                "Registration successful",
                context.authUserId(),
                context.email()
        );
    }
}
