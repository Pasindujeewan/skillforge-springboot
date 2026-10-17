package com.skillforge.userservice.repository;

import com.skillforge.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID> {

    boolean existsByUsername(String username);
}
