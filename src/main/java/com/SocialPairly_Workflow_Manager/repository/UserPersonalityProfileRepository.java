package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserPersonalityProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPersonalityProfileRepository extends JpaRepository<UserPersonalityProfile, Long> {
    Optional<UserPersonalityProfile> findByUserId(Long userId);
}
