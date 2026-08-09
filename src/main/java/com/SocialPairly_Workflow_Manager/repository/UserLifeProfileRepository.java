package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserLifeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLifeProfileRepository extends JpaRepository<UserLifeProfile, Long> {

    Optional<UserLifeProfile> findByUserId(Long userId);
}
