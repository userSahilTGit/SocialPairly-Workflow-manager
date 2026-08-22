package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.validation.EmailOrPhone;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordVerifyOtpRequest(
    @NotBlank(message = "Email or phone number is required")
    @EmailOrPhone
    String identifier,
    @NotBlank(message = "OTP is required")
    String otp
) {}
