package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundListDto(
        Long refundId,
        String formattedRefundId,
        BigDecimal amount,
        String status,
        String action,
        LocalDateTime submissionDate
) {
    public static AdminRefundListDto from(Refund refund) {
        return new AdminRefundListDto(
                refund.getRefundId(),
                refund.getFormattedRefundId(),
                refund.getPayment().getAmount(),
                refund.getStatus() != null ? refund.getStatus().getDisplayValue() : "—",
                refund.getAction() != null ? refund.getAction().getDisplayValue() : "—",
                refund.getCreatedAt()
        );
    }
}
