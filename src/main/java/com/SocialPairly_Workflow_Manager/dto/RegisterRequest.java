package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 60) String firstName,
    @NotBlank @Size(max = 60) String lastName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 7, max = 20) String phoneNumber,
    @NotBlank @Size(min = 6, max = 100) String password,
    @Size(max = 255) String address
) {}