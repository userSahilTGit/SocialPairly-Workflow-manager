package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    @Query("""
            SELECT p FROM UserProfile p
            JOIN FETCH p.user u
            WHERE p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
              AND u.role = :role
            """)
    List<UserProfile> findUserProfilesWithCoordinates(@Param("role") Role role);
}
