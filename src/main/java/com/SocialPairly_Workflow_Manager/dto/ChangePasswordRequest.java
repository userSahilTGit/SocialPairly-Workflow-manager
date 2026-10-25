package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "Password must be at least 6 characters")
        String newPassword,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,
        String otp
) {
    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this(currentPassword, newPassword, confirmPassword, null);
    }
}
