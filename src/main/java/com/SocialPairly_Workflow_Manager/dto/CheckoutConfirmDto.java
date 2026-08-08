package com.SocialPairly_Workflow_Manager.dto;

public record CheckoutConfirmDto(
        SubscriptionDto subscription,
        ReceiptDto receipt,
        String paymentStatus
) {}
