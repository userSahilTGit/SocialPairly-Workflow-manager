package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.BankDetails;

public record BankDetailsDto(
        String accountHolderName,
        String bankName,
        String accountNumber,
        String accountType,
        String abaRoutingNumber,
        String recipientsAddress
) {
    public static BankDetailsDto from(BankDetails details) {
        return new BankDetailsDto(
                details.getAccountHolderName(),
                details.getBankName(),
                details.getAccountNumber(),
                details.getAccountType(),
                details.getAbaRoutingNumber(),
                details.getRecipientsAddress()
        );
    }
}
