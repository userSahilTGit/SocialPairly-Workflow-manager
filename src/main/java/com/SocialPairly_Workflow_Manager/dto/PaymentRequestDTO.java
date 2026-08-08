package com.SocialPairly_Workflow_Manager.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {
    private Long amount;
    private Long quantity;
    private String name;
    private String currency;
    private Long planId;
}
