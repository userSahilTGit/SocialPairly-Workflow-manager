package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

// identifier can be either email or phone number
public record LoginRequest(
    @NotBlank String identifier,
    @NotBlank String password
) {}