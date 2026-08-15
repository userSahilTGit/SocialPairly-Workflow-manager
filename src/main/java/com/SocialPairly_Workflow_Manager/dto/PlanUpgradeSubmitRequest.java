package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanUpgradeSubmitRequest(
        @NotNull(message = "Upgrade plan is required")
        Long upgradePlanId,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
