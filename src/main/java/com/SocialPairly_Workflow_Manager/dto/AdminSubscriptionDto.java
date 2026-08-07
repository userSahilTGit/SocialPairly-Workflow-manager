package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;

import java.time.LocalDateTime;

public record AdminSubscriptionDto(
        Long id,
        Long userId,
        String userName,
        Long planId,
        String planName,
        LocalDateTime subscriptionStartDate,
        LocalDateTime subscriptionEndDate,
        String status
) {
    public static AdminSubscriptionDto from(Subscription subscription) {
        User user = subscription.getUser();
        String userName = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();

        return new AdminSubscriptionDto(
                subscription.getId(),
                user.getId(),
                userName.isEmpty() ? user.getEmail() : userName,
                subscription.getPlan().getId(),
                subscription.getPlan().getPlanName(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getStatus()
        );
    }
}
