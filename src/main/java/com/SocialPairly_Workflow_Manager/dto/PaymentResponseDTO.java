package com.SocialPairly_Workflow_Manager.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponseDTO {
    private String status;
    private String message;
    private String sessionId;
    private String sessionUrl;
}
