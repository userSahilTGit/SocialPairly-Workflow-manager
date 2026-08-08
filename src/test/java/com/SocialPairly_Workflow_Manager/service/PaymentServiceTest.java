package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private PaymentService paymentService;
    private MockedStatic<Session> sessionStatic;
    private MockedStatic<Stripe> stripeStatic;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @BeforeEach
    public void setUp() {
        paymentService = new PaymentService(currentUserService, planRepository, subscriptionService);
        ReflectionTestUtils.setField(paymentService, "secretKey", "test-secret");
        ReflectionTestUtils.setField(paymentService, "frontendUrl", "http://localhost:3000");
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
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        Plan plan = new Plan();
        plan.setId(10L);
        plan.setPlanName("Coffee");
        plan.setDurationDays(7);
        plan.setAmount(new BigDecimal("50.00"));

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));

        PaymentRequestDTO request = new PaymentRequestDTO(5000L, 2L, "Coffee", "USD", 10L);

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
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        Plan plan = new Plan();
        plan.setId(10L);
        plan.setPlanName("T-shirt");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("12.00"));

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));

        PaymentRequestDTO request = new PaymentRequestDTO(1200L, 1L, "T-shirt", "EUR", 10L);

        StripeException mockException = mock(StripeException.class);
        when(mockException.getMessage()).thenReturn("failed");

        sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                .thenThrow(mockException);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            paymentService.checkoutProducts(request);
        });

        assertTrue(ex.getMessage().contains("Unable to create payment session"));
    }

    @Test
    public void checkoutProducts_shouldThrowBadRequestException_whenUserHasActiveSubscription() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        doThrow(new BadRequestException("You already have an active subscription."))
                .when(subscriptionService).ensureNoActiveSubscription(user);

        PaymentRequestDTO request = new PaymentRequestDTO(5000L, 1L, "Coffee", "USD", 10L);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> paymentService.checkoutProducts(request));

        assertTrue(ex.getMessage().contains("already have an active subscription"));
        sessionStatic.verifyNoInteractions();
    }
}
