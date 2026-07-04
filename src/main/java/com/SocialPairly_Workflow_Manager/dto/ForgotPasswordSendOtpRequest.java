package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordSendOtpRequest(
    @NotBlank(message = "Email or phone number is required")
    String identifier
) {}