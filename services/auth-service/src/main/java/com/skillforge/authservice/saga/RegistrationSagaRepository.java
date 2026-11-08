package com.skillforge.authservice.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface RegistrationSagaRepository extends JpaRepository<RegistrationSaga, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select saga from RegistrationSaga saga where saga.id = :id")
    Optional<RegistrationSaga> findByIdForUpdate(@Param("id") UUID id);

    List<RegistrationSaga> findTop20ByStatusOrderByUpdatedAtAsc(
            RegistrationSagaStatus status
    );
}