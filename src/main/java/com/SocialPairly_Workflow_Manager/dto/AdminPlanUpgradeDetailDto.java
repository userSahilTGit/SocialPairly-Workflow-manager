package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;
import com.SocialPairly_Workflow_Manager.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPlanUpgradeDetailDto(
        Long id,
        String formattedUpgradeId,
        Long userId,
        String userName,
        String userEmail,
        String currentPlan,
        String upgradePlan,
        String reason,
        Integer existingTokens,
        Integer extraToken,
        BigDecimal extraAmount,
        String status,
        String action,
        LocalDateTime createdAt
) {
    public static AdminPlanUpgradeDetailDto from(PlanUpgradeRequest request) {
        User user = request.getUser();
        String userName = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();

        return new AdminPlanUpgradeDetailDto(
                request.getId(),
                request.getFormattedUpgradeId(),
                user.getId(),
                userName.isEmpty() ? user.getEmail() : userName,
                user.getEmail(),
                request.getCurrentPlan(),
                request.getUpgradePlan(),
                request.getReason(),
                user.getUserTokens(),
                request.getExtraToken(),
                request.getExtraAmount(),
                request.getStatus() != null ? request.getStatus().getDisplayValue() : null,
                request.getAction() != null ? request.getAction().getDisplayValue() : null,
                request.getCreatedAt()
        );
    }
}
