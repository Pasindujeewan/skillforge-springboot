package com.skillforge.authservice.saga;

public enum RegistrationSagaStatus {
    STARTED,
    AUTH_USER_CREATED,
    PROFILE_CREATED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}