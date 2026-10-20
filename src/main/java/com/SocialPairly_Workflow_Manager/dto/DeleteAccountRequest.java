package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 4, message = "OTP must be 4 digits")
    @Pattern(regexp = "\\d{4}", message = "OTP must be 4 digits")
    String otp
) {}
