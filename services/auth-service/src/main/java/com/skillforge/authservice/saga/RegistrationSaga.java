package com.skillforge.authservice.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_sagas")
@Getter
public class RegistrationSaga {

    @Id
    private UUID id;

    @Column(name = "auth_user_id")
    private Long authUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegistrationSagaStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected RegistrationSaga() {
    }

    private RegistrationSaga(UUID id) {
        this.id = id;
        this.status = RegistrationSagaStatus.STARTED;
    }

    public static RegistrationSaga start() {
        return new RegistrationSaga(UUID.randomUUID());
    }

    public void authUserCreated(Long userId) {
        this.authUserId = userId;
        this.status = RegistrationSagaStatus.AUTH_USER_CREATED;
    }

    public void profileCreated() {
        this.status = RegistrationSagaStatus.PROFILE_CREATED;
    }

    public void complete() {
        this.status = RegistrationSagaStatus.COMPLETED;
        this.failureReason = null;
    }

    public void beginCompensation() {
        this.status = RegistrationSagaStatus.COMPENSATING;
    }

    public void compensationFailed(Throwable failure) {
        this.status = RegistrationSagaStatus.COMPENSATION_FAILED;
        this.failureReason = failure.getClass().getSimpleName();
    }

    public void compensated() {
        this.status = RegistrationSagaStatus.COMPENSATED;
        this.failureReason = null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
