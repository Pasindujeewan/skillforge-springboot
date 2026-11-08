package com.skillforge.authservice.saga;

import java.util.UUID;

public record RegistrationSagaContext(
        UUID sagaId,
        Long authUserId,
        String email
) {
}
