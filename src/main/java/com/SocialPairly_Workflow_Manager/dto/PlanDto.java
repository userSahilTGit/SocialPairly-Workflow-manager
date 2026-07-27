package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Plan;

import java.math.BigDecimal;

public record PlanDto(
    Long id,
    String planName,
    String planType,
    Integer durationDays,
    BigDecimal amount,
    String tokensIncluded,
    String communityPosts,
    String subBadgeTag,
    String description,
    Boolean isActive,
    Boolean isFeatured
) {
    public static PlanDto from(Plan plan) {
        return new PlanDto(
            plan.getId(),
            plan.getPlanName(),
            plan.getPlanType(),
            plan.getDurationDays(),
            plan.getAmount(),
            plan.getTokensIncluded(),
            plan.getCommunityPosts(),
            plan.getSubBadgeTag(),
            plan.getDescription(),
            plan.isActive(),
            plan.isFeatured()
        );
    }
}
