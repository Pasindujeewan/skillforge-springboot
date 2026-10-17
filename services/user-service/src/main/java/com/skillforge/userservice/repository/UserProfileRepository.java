package com.skillforge.userservice.repository;

import com.skillforge.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, Long> {

    boolean existsByUsername(String username);
}
