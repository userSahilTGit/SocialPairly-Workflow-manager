package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebasePhoneVerificationRequest(
    @NotBlank(message = "Firebase idToken is required")
    String idToken
) {}
