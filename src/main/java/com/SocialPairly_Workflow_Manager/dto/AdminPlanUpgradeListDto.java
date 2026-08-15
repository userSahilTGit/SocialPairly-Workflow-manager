package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;

import java.time.LocalDateTime;

public record AdminPlanUpgradeListDto(
        Long id,
        String formattedUpgradeId,
        Long userId,
        String upgradePlan,
        String status,
        String action,
        LocalDateTime submissionDate
) {
    public static AdminPlanUpgradeListDto from(PlanUpgradeRequest request) {
        return new AdminPlanUpgradeListDto(
                request.getId(),
                request.getFormattedUpgradeId(),
                request.getUser().getId(),
                request.getUpgradePlan(),
                request.getStatus() != null ? request.getStatus().getDisplayValue() : "—",
                request.getAction() != null ? request.getAction().getDisplayValue() : "—",
                request.getCreatedAt()
        );
    }
}
