package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserIdentityCompliance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityComplianceRepository extends JpaRepository<UserIdentityCompliance, Long> {

    Optional<UserIdentityCompliance> findByUserId(Long userId);

    Optional<UserIdentityCompliance> findBySessionId(String sessionId);
}
