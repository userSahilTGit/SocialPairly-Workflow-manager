package com.SocialPairly_Workflow_Manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentStatusDto(
        String status,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt
) {}
