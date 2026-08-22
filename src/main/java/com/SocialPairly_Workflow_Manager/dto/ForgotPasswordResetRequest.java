package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.validation.EmailOrPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetRequest(
    @NotBlank(message = "Email or phone number is required")
    @EmailOrPhone
    String identifier,
    @NotBlank(message = "OTP is required")
    String otp,
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String newPassword,
    @NotBlank(message = "Confirm password is required")
    String confirmPassword
) {}
