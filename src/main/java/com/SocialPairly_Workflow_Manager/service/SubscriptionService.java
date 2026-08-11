package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminSubscriptionDto;
import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
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
    private final UserTokenService userTokenService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PaymentRepository paymentRepository,
                               CurrentUserService currentUserService,
                               UserTokenService userTokenService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.userTokenService = userTokenService;
    }

    @Transactional
    public Optional<SubscriptionDto> getCurrentSubscription() {
        User user = currentUserService.getCurrentUser();
        Optional<Subscription> subscription = findPrimaryActiveSubscription(user.getId());
        subscription.ifPresent(sub -> userTokenService.ensureSubscriptionTokensCredited(user, sub));
        return subscription.map(SubscriptionDto::from);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long userId) {
        return subscriptionRepository.hasActiveSubscriptionForUser(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findPrimaryActiveSubscription(Long userId) {
        List<Subscription> activeSubscriptions =
                subscriptionRepository.findActiveSubscriptionsForUser(userId, LocalDateTime.now());
        return activeSubscriptions.isEmpty() ? Optional.empty() : Optional.of(activeSubscriptions.get(0));
    }

    @Transactional(readOnly = true)
    public void ensureNoActiveSubscription(User user) {
        if (hasActiveSubscription(user.getId())) {
            throw new BadRequestException(
                    "You already have an active subscription. Please wait until your current plan expires or discontinue it before purchasing a new plan."
            );
        }
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
