package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.CheckoutConfirmDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutFulfillmentServiceAdditionalTests {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CheckoutFulfillmentService checkoutFulfillmentService;

    private MockedStatic<Session> sessionStatic;
    private MockedStatic<PaymentIntent> paymentIntentStatic;
    private MockedStatic<Charge> chargeStatic;
    private MockedStatic<com.stripe.model.PaymentMethod> paymentMethodStatic;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(checkoutFulfillmentService, "secretKey", "sk_test");
        Stripe.apiKey = "sk_test";
        sessionStatic = mockStatic(Session.class);
        paymentIntentStatic = mockStatic(PaymentIntent.class);
        chargeStatic = mockStatic(Charge.class);
        paymentMethodStatic = mockStatic(com.stripe.model.PaymentMethod.class);

        lenient().when(subscriptionRepository.findActiveSubscriptionsForUser(anyLong(), any()))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        if (sessionStatic != null) sessionStatic.close();
        if (paymentIntentStatic != null) paymentIntentStatic.close();
        if (chargeStatic != null) chargeStatic.close();
        if (paymentMethodStatic != null) paymentMethodStatic.close();
    }

    @Test
    void fulfillCheckoutSessionShouldDelegateToFulfillPaidSession() throws StripeException {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of("userId", "1", "planId", "5"));
        when(session.getId()).thenReturn("sess_ok");
        when(session.getCustomer()).thenReturn("cus_1");
        when(session.getPaymentIntent()).thenReturn(null);
        when(session.getAmountTotal()).thenReturn(1000L);
        when(session.getCurrency()).thenReturn("usd");

        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setPlanName("Basic");
        plan.setDurationDays(7);
        plan.setAmount(new BigDecimal("10.00"));

        sessionStatic.when(() -> Session.retrieve("sess_ok")).thenReturn(session);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_ok")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription sub = inv.getArgument(0);
            sub.setId(1L);
            return sub;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillCheckoutSession("sess_ok", 1L);

        assertNotNull(result);
        assertEquals("succeeded", result.paymentStatus());
    }

    @Test
    void privateFormattingHelpersShouldCoverAllPaymentMethodBranches() throws StripeException {
        Charge.PaymentMethodDetails affirmDetails = mock(Charge.PaymentMethodDetails.class);
        when(affirmDetails.getType()).thenReturn("affirm");
        assertEquals("Affirm", invoke("formatPaymentMethodDetails", affirmDetails));

        Charge.PaymentMethodDetails linkDetails = mock(Charge.PaymentMethodDetails.class);
        when(linkDetails.getType()).thenReturn("link");
        when(linkDetails.getLink()).thenReturn(mock(Charge.PaymentMethodDetails.Link.class));
        assertEquals("Link", invoke("formatPaymentMethodDetails", linkDetails));

        Charge.PaymentMethodDetails bankDetails = mock(Charge.PaymentMethodDetails.class);
        Charge.PaymentMethodDetails.UsBankAccount usBank = mock(Charge.PaymentMethodDetails.UsBankAccount.class);
        when(bankDetails.getType()).thenReturn("us_bank_account");
        when(bankDetails.getUsBankAccount()).thenReturn(usBank);
        when(usBank.getLast4()).thenReturn("6789");
        assertEquals("Bank account - 6789", invoke("formatPaymentMethodDetails", bankDetails));

        Charge.PaymentMethodDetails genericDetails = mock(Charge.PaymentMethodDetails.class);
        when(genericDetails.getType()).thenReturn("klarna");
        assertEquals("Klarna", invoke("formatPaymentMethodDetails", genericDetails));

        assertNull(invokeFormatPaymentMethodDetails(null));

        Charge.PaymentMethodDetails nullTypeDetails = mock(Charge.PaymentMethodDetails.class);
        when(nullTypeDetails.getType()).thenReturn(null);
        assertNull(invoke("formatPaymentMethodDetails", nullTypeDetails));

        com.stripe.model.PaymentMethod cardMethod = mock(com.stripe.model.PaymentMethod.class);
        com.stripe.model.PaymentMethod.Card card = mock(com.stripe.model.PaymentMethod.Card.class);
        when(cardMethod.getType()).thenReturn("card");
        when(cardMethod.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn("visa");
        when(card.getLast4()).thenReturn("1111");
        assertEquals("VISA - 1111", invoke("formatStripePaymentMethod", cardMethod));

        com.stripe.model.PaymentMethod cardOnly = mock(com.stripe.model.PaymentMethod.class);
        when(cardOnly.getType()).thenReturn("card");
        when(cardOnly.getCard()).thenReturn(null);
        assertEquals("Card", invoke("formatStripePaymentMethod", cardOnly));

        com.stripe.model.PaymentMethod affirmMethod = mock(com.stripe.model.PaymentMethod.class);
        when(affirmMethod.getType()).thenReturn("affirm");
        assertEquals("Affirm", invoke("formatStripePaymentMethod", affirmMethod));

        com.stripe.model.PaymentMethod linkMethod = mock(com.stripe.model.PaymentMethod.class);
        when(linkMethod.getType()).thenReturn("link");
        assertEquals("Link", invoke("formatStripePaymentMethod", linkMethod));

        com.stripe.model.PaymentMethod usBankMethod = mock(com.stripe.model.PaymentMethod.class);
        com.stripe.model.PaymentMethod.UsBankAccount bank = mock(com.stripe.model.PaymentMethod.UsBankAccount.class);
        when(usBankMethod.getType()).thenReturn("us_bank_account");
        when(usBankMethod.getUsBankAccount()).thenReturn(bank);
        when(bank.getLast4()).thenReturn("4321");
        assertEquals("Bank account - 4321", invoke("formatStripePaymentMethod", usBankMethod));

        com.stripe.model.PaymentMethod usBankFallback = mock(com.stripe.model.PaymentMethod.class);
        when(usBankFallback.getType()).thenReturn("us_bank_account");
        when(usBankFallback.getUsBankAccount()).thenReturn(null);
        assertEquals("Bank account", invoke("formatStripePaymentMethod", usBankFallback));

        com.stripe.model.PaymentMethod defaultMethod = mock(com.stripe.model.PaymentMethod.class);
        when(defaultMethod.getType()).thenReturn("cash_app");
        assertEquals("Cash app", invoke("formatStripePaymentMethod", defaultMethod));

        assertNull(invokeFormatStripePaymentMethod(null));

        assertEquals("Card", invokeStringMethod("formatCardBrand", null));
        assertEquals("Card", invokeStringMethod("formatCardBrand", ""));
        assertEquals("MASTERCARD", invokeStringMethod("formatCardBrand", "mastercard"));

        assertNull(invokeStringMethod("capitalize", null));
        assertEquals("", invokeStringMethod("capitalize", ""));
        assertEquals("Hello", invokeStringMethod("capitalize", "hello"));
    }

    @Test
    void resolvePaymentMethodFromIntentShouldCoverAffirmAndGenericTypes() throws StripeException {
        PaymentIntent intentWithAffirm = mock(PaymentIntent.class);
        when(intentWithAffirm.getPaymentMethod()).thenReturn(null);
        when(intentWithAffirm.getPaymentMethodTypes()).thenReturn(List.of("affirm"));
        assertEquals("Affirm", invoke("resolvePaymentMethodFromIntent", intentWithAffirm));

        PaymentIntent intentWithGeneric = mock(PaymentIntent.class);
        when(intentWithGeneric.getPaymentMethod()).thenReturn(null);
        when(intentWithGeneric.getPaymentMethodTypes()).thenReturn(List.of("afterpay_clearpay"));
        assertEquals("Afterpay clearpay", invoke("resolvePaymentMethodFromIntent", intentWithGeneric));

        PaymentIntent intentWithMethod = mock(PaymentIntent.class);
        when(intentWithMethod.getPaymentMethod()).thenReturn("pm_123");
        com.stripe.model.PaymentMethod pm = mock(com.stripe.model.PaymentMethod.class);
        com.stripe.model.PaymentMethod.Card card = mock(com.stripe.model.PaymentMethod.Card.class);
        when(pm.getType()).thenReturn("card");
        when(pm.getCard()).thenReturn(card);
        when(card.getBrand()).thenReturn("amex");
        when(card.getLast4()).thenReturn("9999");
        paymentMethodStatic.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_123")).thenReturn(pm);
        assertEquals("AMEX - 9999", invoke("resolvePaymentMethodFromIntent", intentWithMethod));

        PaymentIntent intentWithMethodError = mock(PaymentIntent.class);
        when(intentWithMethodError.getPaymentMethod()).thenReturn("pm_err");
        StripeException stripeException = mock(StripeException.class);
        paymentMethodStatic.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_err")).thenThrow(stripeException);
        when(intentWithMethodError.getPaymentMethodTypes()).thenReturn(List.of("card"));
        assertEquals("Card", invoke("resolvePaymentMethodFromIntent", intentWithMethodError));
    }

    @Test
    void fetchHelpersShouldHandleStripeErrorsAndInvalidIds() throws StripeException {
        assertNull(invoke("fetchReceiptNumber", "invalid"));
        assertNull(invoke("fetchReceiptUrl", "invalid"));

        StripeException stripeException = mock(StripeException.class);
        chargeStatic.when(() -> Charge.retrieve("ch_err")).thenThrow(stripeException);
        assertNull(invoke("fetchReceiptNumber", "ch_err"));
        assertNull(invoke("fetchReceiptUrl", "ch_err"));

        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_err")).thenThrow(stripeException);
        assertNull(invokeFetchPaymentMethodLabel("pi_err", "ch_err"));

        Charge charge = mock(Charge.class);
        when(charge.getPaymentMethodDetails()).thenReturn(null);
        chargeStatic.when(() -> Charge.retrieve("ch_ok")).thenReturn(charge);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getPaymentMethod()).thenReturn(null);
        when(intent.getPaymentMethodTypes()).thenReturn(List.of("affirm"));
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_ok")).thenReturn(intent);
        assertEquals("Affirm", invokeFetchPaymentMethodLabel("pi_ok", "ch_ok"));
    }

    @Test
    void buildPaymentFromSessionShouldHandlePaymentIntentRetrievalFailure() throws StripeException {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of("userId", "1", "planId", "5"));
        when(session.getId()).thenReturn("sess_pi_fail");
        when(session.getCustomer()).thenReturn("cus_1");
        when(session.getPaymentIntent()).thenReturn("pi_fail");
        when(session.getAmountTotal()).thenReturn(1000L);
        when(session.getCurrency()).thenReturn("usd");

        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setPlanName("Basic");
        plan.setDurationDays(7);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByStripeSubscriptionId("sess_pi_fail")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription sub = inv.getArgument(0);
            sub.setId(1L);
            return sub;
        });
        when(paymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        StripeException stripeException = mock(StripeException.class);
        paymentIntentStatic.when(() -> PaymentIntent.retrieve("pi_fail")).thenThrow(stripeException);

        CheckoutConfirmDto result = checkoutFulfillmentService.fulfillPaidSession(session, null);

        assertNotNull(result);
        assertEquals("succeeded", result.paymentStatus());
    }

    private Object invoke(String methodName, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Charge.PaymentMethodDetails) {
                paramTypes[i] = Charge.PaymentMethodDetails.class;
            } else if (arg instanceof com.stripe.model.PaymentMethod) {
                paramTypes[i] = com.stripe.model.PaymentMethod.class;
            } else if (arg instanceof PaymentIntent) {
                paramTypes[i] = PaymentIntent.class;
            } else if (arg instanceof String) {
                paramTypes[i] = String.class;
            } else {
                paramTypes[i] = arg != null ? arg.getClass() : Object.class;
            }
        }
        try {
            var method = CheckoutFulfillmentService.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(checkoutFulfillmentService, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeStringMethod(String methodName, String value) {
        try {
            var method = CheckoutFulfillmentService.class.getDeclaredMethod(methodName, String.class);
            method.setAccessible(true);
            return (String) method.invoke(checkoutFulfillmentService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeFormatPaymentMethodDetails(Charge.PaymentMethodDetails details) {
        try {
            var method = CheckoutFulfillmentService.class.getDeclaredMethod(
                    "formatPaymentMethodDetails", Charge.PaymentMethodDetails.class);
            method.setAccessible(true);
            return method.invoke(checkoutFulfillmentService, details);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeFormatStripePaymentMethod(com.stripe.model.PaymentMethod paymentMethod) {
        try {
            var method = CheckoutFulfillmentService.class.getDeclaredMethod(
                    "formatStripePaymentMethod", com.stripe.model.PaymentMethod.class);
            method.setAccessible(true);
            return method.invoke(checkoutFulfillmentService, paymentMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeFetchPaymentMethodLabel(String paymentIntentId, String chargeId) {
        try {
            var method = CheckoutFulfillmentService.class.getDeclaredMethod(
                    "fetchPaymentMethodLabel", String.class, String.class);
            method.setAccessible(true);
            return method.invoke(checkoutFulfillmentService, paymentIntentId, chargeId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
