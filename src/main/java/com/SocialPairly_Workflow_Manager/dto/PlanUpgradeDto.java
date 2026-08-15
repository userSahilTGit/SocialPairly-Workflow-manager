package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanUpgradeDto(
        Long id,
        String formattedUpgradeId,
        String currentPlan,
        String upgradePlan,
        String reason,
        String status,
        String action,
        Integer extraToken,
        BigDecimal extraAmount,
        Integer userTokens,
        Long paymentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PlanUpgradeDto from(PlanUpgradeRequest request, Integer userTokens) {
        return new PlanUpgradeDto(
                request.getId(),
                request.getFormattedUpgradeId(),
                request.getCurrentPlan(),
                request.getUpgradePlan(),
                request.getReason(),
                request.getStatus() != null ? request.getStatus().getDisplayValue() : null,
                request.getAction() != null ? request.getAction().getDisplayValue() : null,
                request.getExtraToken(),
                request.getExtraAmount(),
                userTokens,
                request.getPayment() != null ? request.getPayment().getId() : null,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
