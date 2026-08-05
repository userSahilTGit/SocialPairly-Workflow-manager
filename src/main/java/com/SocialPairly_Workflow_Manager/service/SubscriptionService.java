package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserService currentUserService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               CurrentUserService currentUserService) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDto> getCurrentSubscription() {
        User user = currentUserService.getCurrentUser();
        return subscriptionRepository
                .findActiveSubscriptionForUser(user.getId(), LocalDateTime.now())
                .map(SubscriptionDto::from);
    }
}
