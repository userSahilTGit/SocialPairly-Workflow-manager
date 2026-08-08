package com.SocialPairly_Workflow_Manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptDto(
        String receiptNumber,
        String planName,
        BigDecimal amount,
        String currency,
        LocalDateTime paidAt,
        String customerEmail,
        String receiptUrl,
        String merchantName,
        String supportEmail,
        String paymentMethod
) {}
