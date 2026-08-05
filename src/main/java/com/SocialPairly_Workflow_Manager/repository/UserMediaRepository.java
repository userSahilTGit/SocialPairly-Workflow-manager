package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.UserMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMediaRepository extends JpaRepository<UserMedia, Long> {
    List<UserMedia> findByUserId(Long userId);
    List<UserMedia> findByUserIdOrderByDisplayOrderAsc(Long userId);
    List<UserMedia> findByUserIdAndStatus(Long userId, MediaStatus status);
    List<UserMedia> findByStatus(MediaStatus status);
    List<UserMedia> findAllByOrderByCreatedAtDesc();

    // 👈 Privacy query layer (Step 1) - Expanded with VERIFIED_ONLY and EVENT_ONLY
    // Returns only media a viewer is allowed to see on a target profile:
    //   - Always visible: PUBLIC items that are APPROVED
    //   - Conditionally visible: MUTUAL_ONLY items, only when isMutual=true
    //   - Conditionally visible: VERIFIED_ONLY items, only when viewer is verified
    //   - Conditionally visible: EVENT_ONLY items, only when event is active
    // The isMutual and isVerified flags are resolved by the service layer.
    @Query("SELECT m FROM UserMedia m WHERE m.user.id = :targetUserId AND " +
            "(m.privacyMode = 'PUBLIC' " +
            "OR (:isMutual = true AND m.privacyMode = 'MUTUAL_ONLY') " +
            "OR (:isVerified = true AND m.privacyMode = 'VERIFIED_ONLY') " +
            "OR (:isEventActive = true AND m.privacyMode = 'EVENT_ONLY')) " +
            "AND m.status = 'APPROVED'")
    List<UserMedia> findVisibleMediaForViewer(@Param("targetUserId") Long targetUserId,
                                              @Param("isMutual") boolean isMutual,
                                              @Param("isVerified") boolean isVerified,
                                              @Param("isEventActive") boolean isEventActive);
}
