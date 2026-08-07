package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankDetailsRequestDto(
        @NotBlank(message = "Account holder name is required")
        @Size(max = 255)
        String accountHolderName,

        @NotBlank(message = "Bank name is required")
        @Size(max = 100)
        String bankName,

        @NotBlank(message = "Account number is required")
        @Size(max = 50)
        String accountNumber,

        @NotBlank(message = "Account type is required")
        @Size(max = 30)
        String accountType,

        @NotBlank(message = "Routing code is required")
        @Size(max = 20)
        String abaRoutingNumber,

        @NotBlank(message = "Recipient address is required")
        String recipientsAddress
) {
}
