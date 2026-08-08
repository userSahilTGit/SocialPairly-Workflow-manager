package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.user
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findAllWithUserOrderByCreatedAtDesc();

    List<Payment> findBySubscription_Id(Long subscriptionId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findFirstByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime since
    );

    void deleteByUser_Id(Long userId);
}
