package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String stripeChargeId,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime paymentDate,
        String receiptUrl
) {
    public static AdminPaymentDto from(Payment payment) {
        User user = payment.getUser();
        String userName = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();

        String chargeId = payment.getStripeChargeId();
        if (chargeId == null || chargeId.isBlank()) {
            chargeId = payment.getStripePaymentIntentId();
        }

        String status = resolveStatus(payment);

        return new AdminPaymentDto(
                payment.getId(),
                user.getId(),
                userName.isEmpty() ? user.getEmail() : userName,
                user.getEmail(),
                chargeId,
                payment.getAmount(),
                payment.getCurrency(),
                status,
                payment.getCreatedAt(),
                payment.getReceiptUrl()
        );
    }

    private static String resolveStatus(Payment payment) {
        if (payment.getAmountRefunded() != null
                && payment.getAmountRefunded().compareTo(BigDecimal.ZERO) > 0) {
            return "refunded";
        }
        return payment.getStatus() != null ? payment.getStatus().toLowerCase() : "pending";
    }
}
