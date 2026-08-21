package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.LoginSecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LoginSecurityEventRepository extends JpaRepository<LoginSecurityEvent, Long> {

    @Query("""
            SELECT COUNT(e) FROM LoginSecurityEvent e
            WHERE e.userId = :userId
              AND e.eventType = :eventType
              AND e.createdAt >= :since
            """)
    long countByUserIdAndEventTypeSince(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("since") LocalDateTime since);

    long countByUserIdAndEventType(Long userId, String eventType);
}
