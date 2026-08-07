package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminSubscriptionDto;
import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PaymentRepository paymentRepository,
                               CurrentUserService currentUserService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDto> getCurrentSubscription() {
        User user = currentUserService.getCurrentUser();
        return subscriptionRepository
                .findActiveSubscriptionForUser(user.getId(), LocalDateTime.now())
                .map(SubscriptionDto::from);
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionDto> getAllSubscriptionsForAdmin() {
        return subscriptionRepository.findAllWithUserAndPlan().stream()
                .map(AdminSubscriptionDto::from)
                .toList();
    }

    @Transactional
    public void removeSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        List<Payment> linkedPayments = paymentRepository.findBySubscription_Id(subscriptionId);
        for (Payment payment : linkedPayments) {
            payment.setSubscription(null);
        }
        paymentRepository.saveAll(linkedPayments);
        subscriptionRepository.delete(subscription);
    }
}
