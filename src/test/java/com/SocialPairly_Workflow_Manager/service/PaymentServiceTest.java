package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private PaymentService paymentService;
    private MockedStatic<Session> sessionStatic;
    private MockedStatic<Stripe> stripeStatic;

    @BeforeEach
    public void setUp() {
        paymentService = new PaymentService();
        stripeStatic = mockStatic(Stripe.class);
        sessionStatic = mockStatic(Session.class);
        Stripe.apiKey = "test-secret";
    }

    @AfterEach
    public void tearDown() {
        if (sessionStatic != null) {
            sessionStatic.close();
        }
        if (stripeStatic != null) {
            stripeStatic.close();
        }
    }

    @Test
    public void checkoutProducts_shouldReturnSuccessResponse_whenStripeSessionCreated() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(5000L, 2L, "Coffee", "USD");

        Session mockSession = new Session();
        mockSession.setId("sess_123");
        mockSession.setUrl("https://checkout.stripe.com/session/sess_123");

        sessionStatic.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

        PaymentResponseDTO response = paymentService.checkoutProducts(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Payment session created ", response.getMessage());
        assertEquals("sess_123", response.getSessionId());
        assertEquals("https://checkout.stripe.com/session/sess_123", response.getSessionUrl());
    }

    @Test
    public void checkoutProducts_shouldThrowIllegalStateException_whenStripeThrowsException() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(1200L, 1L, "T-shirt", "EUR");

        StripeException mockException = mock(StripeException.class);
        when(mockException.getMessage()).thenReturn("failed");

        sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                .thenThrow(mockException);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            paymentService.checkoutProducts(request);
        });

        assertTrue(ex.getMessage().contains("Unable to create payment session"));
    }
}
