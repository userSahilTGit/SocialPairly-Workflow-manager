package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.plan
            WHERE s.user.id = :userId
              AND s.status = 'active'
              AND s.currentPeriodEnd > :now
            ORDER BY s.currentPeriodEnd DESC
            """)
    Optional<Subscription> findActiveSubscriptionForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
