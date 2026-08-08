package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.BankDetails;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundDetailDto(
        Long refundId,
        String formattedRefundId,
        String userName,
        String userEmail,
        BigDecimal claimAmount,
        String status,
        String action,
        String reason,
        LocalDateTime slot,
        LocalDateTime createdAt,
        BankDetailsDto bankDetails
) {
    public static AdminRefundDetailDto from(Refund refund, BankDetails bankDetails) {
        User user = refund.getUser();
        String userName = ((user.getFirstName() != null ? user.getFirstName() : "")
                + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();

        return new AdminRefundDetailDto(
                refund.getRefundId(),
                refund.getFormattedRefundId(),
                userName.isEmpty() ? user.getEmail() : userName,
                user.getEmail(),
                refund.getPayment().getAmount(),
                refund.getStatus() != null ? refund.getStatus().getDisplayValue() : null,
                refund.getAction() != null ? refund.getAction().getDisplayValue() : null,
                refund.getReason(),
                refund.getSlot(),
                refund.getCreatedAt(),
                bankDetails != null ? BankDetailsDto.from(bankDetails) : null
        );
    }
}
