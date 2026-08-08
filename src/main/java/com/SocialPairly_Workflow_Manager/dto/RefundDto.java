package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.RefundAction;
import com.SocialPairly_Workflow_Manager.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundDto(
        Long refundId,
        String formattedRefundId,
        String status,
        String action,
        String reason,
        BigDecimal amount,
        String currency,
        LocalDateTime slot,
        LocalDateTime createdAt,
        BigDecimal refundAmount
) {
    public static RefundDto from(Refund refund) {
        BigDecimal paymentAmount = refund.getPayment().getAmount();
        BigDecimal refundedAmount = refund.getPayment().getAmountRefunded();

        return new RefundDto(
                refund.getRefundId(),
                refund.getFormattedRefundId(),
                refund.getStatus() != null ? refund.getStatus().getDisplayValue() : null,
                refund.getAction() != null ? refund.getAction().getDisplayValue() : null,
                refund.getReason(),
                paymentAmount,
                refund.getPayment().getCurrency(),
                refund.getSlot(),
                refund.getCreatedAt(),
                refundedAmount != null && refundedAmount.compareTo(BigDecimal.ZERO) > 0
                        ? refundedAmount
                        : null
        );
    }

    public static RefundDto from(Refund refund, BigDecimal netRefundAmount) {
        RefundDto base = from(refund);
        return new RefundDto(
                base.refundId(),
                base.formattedRefundId(),
                base.status(),
                base.action(),
                base.reason(),
                base.amount(),
                base.currency(),
                base.slot(),
                base.createdAt(),
                netRefundAmount
        );
    }
}
