package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("""
            SELECT DISTINCT s.user.id FROM Subscription s
            WHERE s.status = 'active'
              AND s.currentPeriodEnd > :now
            """)
    List<Long> findActiveSubscribedUserIds(@Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.user
            JOIN FETCH s.plan
            ORDER BY s.currentPeriodStart DESC
            """)
    List<Subscription> findAllWithUserAndPlan();

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.plan
            WHERE s.user.id = :userId
              AND s.status = 'active'
              AND s.currentPeriodEnd > :now
            ORDER BY s.currentPeriodEnd DESC
            """)
    List<Subscription> findActiveSubscriptionsForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Subscription s
            WHERE s.user.id = :userId
              AND s.status = 'active'
              AND s.currentPeriodEnd > :now
            """)
    boolean hasActiveSubscriptionForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
