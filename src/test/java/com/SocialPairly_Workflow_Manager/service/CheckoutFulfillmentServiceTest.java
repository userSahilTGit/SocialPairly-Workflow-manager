package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.CheckoutConfirmDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutFulfillmentServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private CheckoutFulfillmentService checkoutFulfillmentService;

    private MockedStatic<Session> sessionStatic;
    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;

    private User user;
    private Plan plan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(checkoutFulfillmentService, "secretKey", "sk_test");
        ReflectionTestUtils.setField(checkoutFulfillmentService, "merchantName", "SocialPairly");
        ReflectionTestUtils.setField(checkoutFulfillmentService, "supportEmail", "support@test.com");
        Stripe.apiKey = "sk_test";

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        plan = new Plan();
        plan.setId(5L);
        plan.setPlanName("Pro");
        plan.setPlanType("MONTHLY");
        plan.setDurationDays(30);
        plan.setAmount(new BigDecimal("29.99"));

        sessionStatic = mockStatic(Session.class);
        paymentIntentStatic = mockStatic(PaymentIntent.class);
        chargeStatic = mockStatic(Charge.class);
    }

    @AfterEach
    void tearDown() {
        if (sessionStatic != null) sessionStatic.close();
        if (paymentIntentStatic != null) paymentIntentStatic.close();
        if (chargeStatic != null) chargeStatic.close();
    }

    @Test
    void centsToDollarsShouldConvertAndHandleNull() {
        assertEquals(new BigDecimal("29.99"), checkoutFulfillmentService.centsToDollars(2999L));
        assertEquals(new BigDecimal("0.00"), checkoutFulfillmentService.centsToDollars(null));
    }

    @Test
    void fulfillCheckoutSessionShouldThrowWhenStripeRetrievalFails() throws StripeException {
        StripeException stripeException = mock(StripeException.class);
        sessionStatic.when(() -> Session.retrieve("sess_bad")).thenThrow(stripeException);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutFulfillmentService.fulfillCheckoutSession("sess_bad", 1L));
        assertEquals("Invalid checkout session", ex.getMessage());
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenNotPaid() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("unpaid");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, 1L));
        assertTrue(ex.getMessage().contains("not paid"));
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenUserMismatch() {
        Session session = createPaidSession("2", "5");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, 1L));
        assertEquals("Checkout session does not belong to the current user", ex.getMessage());
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenMetadataMissing() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(new HashMap<>());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, null));
        assertTrue(ex.getMessage().contains("Missing Stripe metadata field"));
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenMetadataInvalid() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of("userId", "abc", "planId", "5"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, null));
        assertTrue(ex.getMessage().contains("Invalid Stripe metadata"));
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenUserNotFound() {
        Session session = createPaidSession("1", "5");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, null));
    }

    @Test
    void fulfillPaidSessionShouldThrowWhenPlanNotFound() {
        Session session = createPaidSession("1", "5");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> checkoutFulfillmentService.fulfillPaidSession(session, null));
    }

    @Test
    void fulfillPaidSessionShouldCreateSubscriptionAndPayment() throws StripeException {
        Session session = createPaidSession("1", "5");
        when(session.getId()).thenReturn("sess_new");
        when(session.getCustomer()).thenReturn("cus_123");
        when(session.getPaymentIntent()).thenReturn("pi_new");
        when(session.getAmountTotal()).thenReturn(2999L);
        when(session.getCurrency()).thenReturn("usd");

        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getLatestCharge()).thenReturn("ch_new");
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_new")).thenReturn(paymentIntent);

        Charge charge = mock(Charge.class);
        when(charge.getReceiptUrl()).thenReturn("https://receipt.example.com");
        when(charge.getReceiptNumber()).thenReturn("RCPT-1");
        chargeStatic.when(() -> Charge.retrieve("ch_new")).thenReturn(charge);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_new")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription sub = inv.getArgument(0);
            sub.setId(10L);
            return sub;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_new")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillPaidSession(session, 1L);

        assertNotNull(result);
        assertEquals("succeeded", result.paymentStatus());
        assertEquals("Pro", result.subscription().planName());
        assertEquals("user@test.com", result.receipt().customerEmail());
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void fulfillPaidSessionShouldReuseExistingSubscription() throws StripeException {
        Session session = createPaidSession("1", "5");
        when(session.getId()).thenReturn("sess_existing");
        when(session.getPaymentIntent()).thenReturn("pi_existing");

        Subscription existingSub = new Subscription();
        existingSub.setId(10L);
        existingSub.setUser(user);
        existingSub.setPlan(plan);
        existingSub.setStatus("active");

        Payment existingPayment = new Payment();
        existingPayment.setId(20L);
        existingPayment.setUser(user);
        existingPayment.setSubscription(existingSub);
        existingPayment.setStripePaymentIntentId("pi_existing");
        existingPayment.setStripeChargeId("ch_existing");
        existingPayment.setAmount(new BigDecimal("29.99"));
        existingPayment.setCurrency("usd");
        existingPayment.setStatus("succeeded");
        existingPayment.setMethod("VISA - 4242");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_existing"))
                .thenReturn(Optional.of(existingSub));
        when(paymentRepository.findByStripePaymentIntentId("pi_existing"))
                .thenReturn(Optional.of(existingPayment));

        Charge charge = mock(Charge.class);
        when(charge.getReceiptNumber()).thenReturn("RCPT-2");
        chargeStatic.when(() -> Charge.retrieve("ch_existing")).thenReturn(charge);

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillPaidSession(session, null);

        assertEquals(10L, result.subscription().id());
        verify(subscriptionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void fulfillPaidSessionShouldUpdateExistingPaymentMissingSubscriptionAndMethod() throws StripeException {
        Session session = createPaidSession("1", "5");
        when(session.getId()).thenReturn("sess_update");
        when(session.getPaymentIntent()).thenReturn("pi_update");

        Subscription existingSub = new Subscription();
        existingSub.setId(10L);
        existingSub.setUser(user);
        existingSub.setPlan(plan);
        existingSub.setStatus("active");

        Payment existingPayment = new Payment();
        existingPayment.setId(20L);
        existingPayment.setUser(user);
        existingPayment.setStripePaymentIntentId("pi_update");
        existingPayment.setStripeChargeId("ch_update");
        existingPayment.setAmount(new BigDecimal("29.99"));
        existingPayment.setCurrency("usd");
        existingPayment.setStatus("succeeded");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_update"))
                .thenReturn(Optional.of(existingSub));
        when(paymentRepository.findByStripePaymentIntentId("pi_update"))
                .thenReturn(Optional.of(existingPayment));

        Charge charge = mock(Charge.class);
        Charge.PaymentMethodDetails details = mock(Charge.PaymentMethodDetails.class);
        Charge.PaymentMethodDetails.Card card = mock(Charge.PaymentMethodDetails.Card.class);
        when(details.getType()).thenReturn("card");
        when(details.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn("visa");
        when(card.getLast4()).thenReturn("4242");
        when(charge.getPaymentMethodDetails()).thenReturn(details);
        when(charge.getReceiptNumber()).thenReturn("RCPT-3");
        chargeStatic.when(() -> Charge.retrieve("ch_update")).thenReturn(charge);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillPaidSession(session, null);

        assertNotNull(result);
        verify(paymentRepository).save(existingPayment);
        assertEquals(existingSub, existingPayment.getSubscription());
        assertEquals("VISA - 4242", existingPayment.getMethod());
    }

    @Test
    void fulfillPaidSessionShouldHandleSubscriptionRaceCondition() {
        Session session = createPaidSession("1", "5");
        when(session.getId()).thenReturn("sess_race");
        when(session.getCustomer()).thenReturn(null);
        when(session.getPaymentIntent()).thenReturn(null);
        when(session.getAmountTotal()).thenReturn(2999L);
        when(session.getCurrency()).thenReturn("usd");

        Subscription existingSub = new Subscription();
        existingSub.setId(10L);
        existingSub.setUser(user);
        existingSub.setPlan(plan);
        existingSub.setStatus("active");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_race")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(existingSub);
        when(subscriptionRepository.findByStripeSubscriptionId("sess_race"))
                .thenReturn(Optional.empty(), Optional.of(existingSub));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillPaidSession(session, null);

        assertEquals(10L, result.subscription().id());
    }

    @Test
    void recordFailedPaymentIntentShouldSkipWhenAlreadyRecorded() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_exists");
        when(paymentRepository.findByStripePaymentIntentId("pi_exists"))
                .thenReturn(Optional.of(new Payment()));

        checkoutFulfillmentService.recordFailedPaymentIntent(paymentIntent);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordFailedPaymentIntentShouldSkipWhenMetadataMissing() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_no_meta");
        when(paymentIntent.getMetadata()).thenReturn(null);
        when(paymentRepository.findByStripePaymentIntentId("pi_no_meta")).thenReturn(Optional.empty());

        checkoutFulfillmentService.recordFailedPaymentIntent(paymentIntent);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordFailedPaymentIntentShouldSaveFailedPayment() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_failed");
        when(paymentIntent.getMetadata()).thenReturn(Map.of("userId", "1"));
        when(paymentIntent.getLatestCharge()).thenReturn("ch_failed");
        when(paymentIntent.getAmount()).thenReturn(5000L);
        when(paymentIntent.getCurrency()).thenReturn("usd");

        when(paymentRepository.findByStripePaymentIntentId("pi_failed")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Charge charge = mock(Charge.class);
        Charge.PaymentMethodDetails details = mock(Charge.PaymentMethodDetails.class);
        Charge.PaymentMethodDetails.Card card = mock(Charge.PaymentMethodDetails.Card.class);
        when(details.getType()).thenReturn("card");
        when(details.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn("visa");
        when(card.getLast4()).thenReturn("9999");
        when(charge.getPaymentMethodDetails()).thenReturn(details);
        chargeStatic.when(() -> Charge.retrieve("ch_failed")).thenReturn(charge);

        checkoutFulfillmentService.recordFailedPaymentIntent(paymentIntent);

        verify(paymentRepository).save(argThat(p ->
                "failed".equals(p.getStatus()) && p.getUser().getId().equals(1L)));
    }

    @Test
    void recordFailedPaymentIntentShouldThrowWhenUserNotFound() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_failed");
        when(paymentIntent.getMetadata()).thenReturn(Map.of("userId", "1"));

        when(paymentRepository.findByStripePaymentIntentId("pi_failed")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> checkoutFulfillmentService.recordFailedPaymentIntent(paymentIntent));
    }

    private Session createPaidSession(String userId, String planId) {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of("userId", userId, "planId", planId));
        return session;
    }
}
