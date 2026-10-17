package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Public contact-support payload (Phase1-Stage1). Delivered to {@code customercare@socialpairly.com}.
 */
public record CustomerCareRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        /** One or more issue topics; empty means "General / all issues". */
        List<@Size(max = 60) String> issues,
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(min = 10, max = 5000) String message,
        @Size(max = 500) String pageUrl
) {
}
