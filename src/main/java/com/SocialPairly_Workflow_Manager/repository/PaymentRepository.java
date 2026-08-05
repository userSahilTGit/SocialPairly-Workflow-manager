package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findFirstByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime since
    );
}
