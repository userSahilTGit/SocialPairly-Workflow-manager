package com.SocialPairly_Workflow_Manager.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentDtoTest {

    @Test
    public void paymentRequestDto_shouldSetAndGetProperties() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(2500L);
        request.setQuantity(3L);
        request.setName("Membership");
        request.setCurrency("USD");
        request.setPlanId(5L);

        assertEquals(2500L, request.getAmount());
        assertEquals(3L, request.getQuantity());
        assertEquals("Membership", request.getName());
        assertEquals("USD", request.getCurrency());
        assertEquals(5L, request.getPlanId());
    }

    @Test
    public void paymentResponseDto_builderAndSetters_shouldWork() {
        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .status("SUCCESS")
                .message("Created")
                .sessionId("sess_789")
                .sessionUrl("https://checkout.stripe.com/session/sess_789")
                .build();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Created", response.getMessage());
        assertEquals("sess_789", response.getSessionId());
        assertEquals("https://checkout.stripe.com/session/sess_789", response.getSessionUrl());

        response.setMessage("Updated");
        assertEquals("Updated", response.getMessage());
    }
}
