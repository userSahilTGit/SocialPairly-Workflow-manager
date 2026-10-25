package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordOtpRequest(
        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{4}", message = "Enter the 4-digit verification code from your email")
        String otp
) {
}
