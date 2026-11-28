package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.UserEventNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserEventNotificationRepository extends JpaRepository<UserEventNotification, Long> {

    List<UserEventNotification> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId);

    Optional<UserEventNotification> findByIdAndUserId(Long id, Long userId);

    Optional<UserEventNotification> findByUserIdAndEventIdAndKind(Long userId, Long eventId, String kind);

    List<UserEventNotification> findByEventIdAndDismissedFalse(Long eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEventNotification n SET n.dismissed = true, n.seen = true WHERE n.eventId = :eventId AND n.userId IN :userIds")
    int dismissForUsers(@Param("eventId") Long eventId, @Param("userIds") List<Long> userIds);
}
