package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
    @NotBlank(message = "Email or phone number is required")
    String identifier,
    @NotBlank(message = "Password is required")
    String password
) {}