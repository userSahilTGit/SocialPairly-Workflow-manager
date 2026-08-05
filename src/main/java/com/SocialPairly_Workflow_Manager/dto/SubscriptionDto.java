package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Subscription;

import java.time.LocalDateTime;

public record SubscriptionDto(
        Long id,
        Long planId,
        String planName,
        String planType,
        Integer durationDays,
        String status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
    public static SubscriptionDto from(Subscription subscription) {
        return new SubscriptionDto(
                subscription.getId(),
                subscription.getPlan().getId(),
                subscription.getPlan().getPlanName(),
                subscription.getPlan().getPlanType(),
                subscription.getPlan().getDurationDays(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd()
        );
    }
}
