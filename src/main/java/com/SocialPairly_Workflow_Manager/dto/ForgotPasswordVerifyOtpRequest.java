package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordVerifyOtpRequest(
    @NotBlank(message = "Email or phone number is required")
    String identifier,
    @NotBlank(message = "OTP is required")
    String otp
) {}