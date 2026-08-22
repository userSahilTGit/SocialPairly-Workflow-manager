package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTokenServiceAdditionalTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private UserTokenService userTokenService;

    private User user;
    private Plan plan;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUserTokens(100);
        plan = new Plan();
        plan.setId(5L);
        plan.setTokensIncluded("500");
    }

    @Test
    void getBalanceShouldReturnZeroForNullUser() {
        assertEquals(0, userTokenService.getBalance(null));
    }

    @Test
    void getBalanceShouldReturnUserTokens() {
        assertEquals(100, userTokenService.getBalance(user));
    }

    @Test
    void deductTokensShouldReduceBalance() {
        when(userRepository.save(user)).thenReturn(user);

        int balance = userTokenService.deductTokens(user, 30);
        assertEquals(70, balance);
        verify(userRepository).save(user);
    }

    @Test
    void deductTokensShouldRejectInvalidAmount() {
        assertThrows(BadRequestException.class, () -> userTokenService.deductTokens(user, 0));
        assertThrows(BadRequestException.class, () -> userTokenService.deductTokens(user, -1));
    }

    @Test
    void deductTokensShouldRejectInsufficientBalance() {
        assertThrows(BadRequestException.class, () -> userTokenService.deductTokens(user, 200));
    }

    @Test
    void deductTokensShouldUseSingularMessageForOneToken() {
        user.setUserTokens(0);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> userTokenService.deductTokens(user, 1));
        assertTrue(ex.getMessage().contains("1 token but"));
    }

    @Test
    void creditTokensFromPlanShouldAddTokens() {
        when(userRepository.save(user)).thenReturn(user);

        int balance = userTokenService.creditTokensFromPlan(user, plan);
        assertEquals(600, balance);
    }

    @Test
    void creditTokensFromPlanShouldSkipWhenNoNumericTokens() {
        plan.setTokensIncluded("UNLIMITED");
        int balance = userTokenService.creditTokensFromPlan(user, plan);
        assertEquals(100, balance);
        verify(userRepository, never()).save(any());
    }

    @Test
    void creditTokensFromPlanShouldHandleNullPlan() {
        int balance = userTokenService.creditTokensFromPlan(user, null);
        assertEquals(100, balance);
    }

    @Test
    void ensureSubscriptionTokensCreditedShouldProcessPayments() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setPlan(plan);

        Payment payment = new Payment();
        payment.setId(20L);
        payment.setStatus("succeeded");
        payment.setTokensCredited(false);
        payment.setSubscription(subscription);

        when(paymentRepository.findBySubscription_Id(10L)).thenReturn(List.of(payment));
        when(paymentRepository.findUncreditedSucceededPaymentsForUser(1L)).thenReturn(List.of());
        when(userRepository.save(user)).thenReturn(user);
        when(paymentRepository.save(payment)).thenReturn(payment);

        userTokenService.ensureSubscriptionTokensCredited(user, subscription);

        assertTrue(payment.isTokensCredited());
        assertEquals(600, user.getUserTokens());
    }

    @Test
    void ensureSubscriptionTokensCreditedShouldNoOpForNullInputs() {
        userTokenService.ensureSubscriptionTokensCredited(null, null);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void creditTokensForPaymentIfNeededShouldSkipCreditedPayment() {
        Payment payment = new Payment();
        payment.setTokensCredited(true);
        payment.setStatus("succeeded");

        userTokenService.creditTokensForPaymentIfNeeded(user, plan, payment);
        verify(userRepository, never()).save(any());
    }

    @Test
    void creditTokensForPaymentIfNeededShouldSkipNonSucceededPayment() {
        Payment payment = new Payment();
        payment.setTokensCredited(false);
        payment.setStatus("pending");

        userTokenService.creditTokensForPaymentIfNeeded(user, plan, payment);
        verify(userRepository, never()).save(any());
    }

    @Test
    void creditTokensForPaymentIfNeededShouldSkipNullPayment() {
        userTokenService.creditTokensForPaymentIfNeeded(user, plan, null);
        verify(userRepository, never()).save(any());
    }

    @Test
    void ensureSubscriptionTokensCreditedShouldProcessUncreditedPaymentsFromOtherSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setPlan(plan);

        Payment linkedPayment = new Payment();
        linkedPayment.setId(20L);
        linkedPayment.setStatus("succeeded");
        linkedPayment.setTokensCredited(false);
        linkedPayment.setSubscription(subscription);

        Payment uncreditedOther = new Payment();
        uncreditedOther.setId(21L);
        uncreditedOther.setStatus("succeeded");
        uncreditedOther.setTokensCredited(false);
        uncreditedOther.setSubscription(null);

        when(paymentRepository.findBySubscription_Id(10L)).thenReturn(List.of(linkedPayment));
        when(paymentRepository.findUncreditedSucceededPaymentsForUser(1L)).thenReturn(List.of(uncreditedOther));
        when(userRepository.save(user)).thenReturn(user);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        userTokenService.ensureSubscriptionTokensCredited(user, subscription);

        assertTrue(linkedPayment.isTokensCredited());
        assertTrue(uncreditedOther.isTokensCredited());
        assertEquals(1100, user.getUserTokens());
    }

    @Test
    void ensureSubscriptionTokensCreditedShouldSkipWhenPlanMissing() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setPlan(null);

        userTokenService.ensureSubscriptionTokensCredited(user, subscription);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void parsePlanTokensShouldHandleMalformedNumbers() {
        assertEquals(0, UserTokenService.parsePlanTokens("abc"));
        assertEquals(123, UserTokenService.parsePlanTokens("123 tokens"));
    }

    @Test
    void applyPlanTokenClawbackShouldReduceBalanceWhenSufficientTokens() {
        user.setUserTokens(150);
        plan.setTokensIncluded("100");
        when(userRepository.save(user)).thenReturn(user);

        var result = userTokenService.applyPlanTokenClawbackOnRefund(
                user, plan, new java.math.BigDecimal("100.00"));

        assertEquals(50, user.getUserTokens());
        assertEquals(150, result.previousBalance());
        assertEquals(50, result.newBalance());
        assertEquals(100, result.tokensRemoved());
        assertEquals(0, result.excessTokensUsed());
        assertEquals(java.math.BigDecimal.ZERO, result.tokenUsageCost());
        assertFalse(result.hasTokenUsageDeduction());
    }

    @Test
    void applyPlanTokenClawbackShouldChargeAndZeroBalanceWhenInsufficientTokens() {
        user.setUserTokens(30);
        plan.setTokensIncluded("100");
        when(userRepository.save(user)).thenReturn(user);

        var result = userTokenService.applyPlanTokenClawbackOnRefund(
                user, plan, new java.math.BigDecimal("100.00"));

        assertEquals(0, user.getUserTokens());
        assertEquals(70, result.excessTokensUsed());
        assertEquals(new java.math.BigDecimal("70.00"), result.tokenUsageCost());
        assertTrue(result.hasTokenUsageDeduction());
    }

    @Test
    void applyPlanTokenClawbackShouldSkipUnlimitedPlans() {
        user.setUserTokens(200);
        plan.setTokensIncluded("UNLIMITED");

        var result = userTokenService.applyPlanTokenClawbackOnRefund(
                user, plan, new java.math.BigDecimal("100.00"));

        assertEquals(200, user.getUserTokens());
        assertEquals(0, result.tokensRemoved());
        verify(userRepository, never()).save(any());
    }

    @Test
    void applyPlanTokenClawbackNullUserAndNullPlanAndZeroPayment() {
        assertEquals(0, userTokenService.applyPlanTokenClawbackOnRefund(null, plan, null).previousBalance());

        user.setUserTokens(10);
        var skip = userTokenService.applyPlanTokenClawbackOnRefund(user, null, null);
        assertEquals(10, skip.previousBalance());
        assertEquals(0, skip.tokensRemoved());

        plan.setTokensIncluded("100");
        when(userRepository.save(user)).thenReturn(user);
        var zeroPay = userTokenService.applyPlanTokenClawbackOnRefund(user, plan, null);
        assertEquals(0, user.getUserTokens());
        assertEquals(90, zeroPay.excessTokensUsed());
        assertEquals(java.math.BigDecimal.ZERO, zeroPay.tokenUsageCost());
    }

    @Test
    void ensureSubscriptionTokensCreditedSkipsUnrelatedSubscriptionPayments() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setPlan(plan);

        Subscription other = new Subscription();
        other.setId(99L);

        Payment unrelated = new Payment();
        unrelated.setId(21L);
        unrelated.setStatus("succeeded");
        unrelated.setTokensCredited(false);
        unrelated.setSubscription(other);

        when(paymentRepository.findBySubscription_Id(10L)).thenReturn(List.of());
        when(paymentRepository.findUncreditedSucceededPaymentsForUser(1L)).thenReturn(List.of(unrelated));

        userTokenService.ensureSubscriptionTokensCredited(user, subscription);
        assertFalse(unrelated.isTokensCredited());
        verify(userRepository, never()).save(any());
    }

    @Test
    void creditTokensFromPlanLogsWhenPlanPresentButNonNumeric() {
        plan.setTokensIncluded("   ");
        assertEquals(100, userTokenService.creditTokensFromPlan(user, plan));
    }
}
