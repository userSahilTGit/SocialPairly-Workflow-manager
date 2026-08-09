package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyContactRequest(
    @NotBlank String identifier,
    @NotBlank String otp
) {}
