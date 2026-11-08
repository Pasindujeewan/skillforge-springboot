package com.skillforge.authservice.saga;

import com.skillforge.authservice.client.UserProfileClient;
import com.skillforge.authservice.entity.AuthUser;
import com.skillforge.authservice.service.AuthUserCommandService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationSagaCompensationService {

    private static final Logger log =
            LoggerFactory.getLogger(RegistrationSagaCompensationService.class);

    private final AuthUserCommandService authUserCommandService;
    private final RegistrationSagaRepository sagaRepository;
    private final UserProfileClient userProfileClient;

    public boolean compensate(java.util.UUID sagaId) {
        try {
            Optional<AuthUser> user = authUserCommandService.beginCompensation(sagaId);
            if (user.isEmpty()) {
                return true;
            }

            userProfileClient.deleteProfile(user.get());
            authUserCommandService.completeCompensation(sagaId, user.get().getId());
            return true;
        } catch (RuntimeException failure) {
            log.error("Registration saga {} compensation failed", sagaId, failure);
            try {
                authUserCommandService.markCompensationFailed(sagaId, failure);
            } catch (RuntimeException stateFailure) {
                failure.addSuppressed(stateFailure);
                log.error(
                        "Could not persist compensation failure for registration saga {}",
                        sagaId,
                        stateFailure
                );
            }
            return false;
        }
    }

    @Scheduled(fixedDelayString = "${registration-saga.recovery-delay:30000}")
    public void retryFailedCompensations() {
        for (RegistrationSagaStatus status : new RegistrationSagaStatus[]{
                RegistrationSagaStatus.COMPENSATION_FAILED,
                RegistrationSagaStatus.COMPENSATING
        }) {
            for (RegistrationSaga saga : sagaRepository
                    .findTop20ByStatusOrderByUpdatedAtAsc(status)) {
                compensate(saga.getId());
            }
        }
    }
}
