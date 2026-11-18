package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleDashboardAccessRequest(
        @NotBlank String roleCode,
        @NotBlank String dashboardKey,
        @NotNull Boolean canAccess
) {
}
