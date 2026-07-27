package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    public void checkoutProducts_shouldReturnOkResponse_whenServiceReturnsResponse() {
        PaymentRequestDTO request = new PaymentRequestDTO(2000L, 1L, "Book", "USD");
        PaymentResponseDTO serviceResponse = PaymentResponseDTO.builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId("sess_456")
                .sessionUrl("https://checkout.stripe.com/session/sess_456")
                .build();

        when(paymentService.checkoutProducts(any(PaymentRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<PaymentResponseDTO> responseEntity = paymentController.checkoutProducts(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertSame(serviceResponse, responseEntity.getBody());
        assertEquals("sess_456", responseEntity.getBody().getSessionId());
    }
}
