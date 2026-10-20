package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "Password must be at least 6 characters")
        String newPassword,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}
