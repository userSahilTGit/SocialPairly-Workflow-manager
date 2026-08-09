package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserIdentityBackground;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityBackgroundRepository extends JpaRepository<UserIdentityBackground, Long> {

    Optional<UserIdentityBackground> findByUserId(Long userId);
}
