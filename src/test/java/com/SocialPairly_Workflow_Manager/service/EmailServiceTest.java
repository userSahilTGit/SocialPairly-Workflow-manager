package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PlanRepository planRepository;

    private EmailService emailService;

    private User validUser;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, planRepository);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "appName", "SocialPairly");
        ReflectionTestUtils.setField(emailService, "supportEmail", "support@test.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "https://test.com");

        validUser = new User();
        validUser.setFirstName("Jane");
        validUser.setLastName("Doe");
        validUser.setEmail("jane@example.com");

        Session session = Session.getDefaultInstance(new Properties());
        mimeMessage = new MimeMessage(session);
    }

    @Test
    void sendWelcomeEmailShouldSendWhenUserHasValidEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcomeEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendWelcomeEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail("invalid-email");

        emailService.sendWelcomeEmail(user);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendWelcomeEmailShouldUseThereWhenNameMissing() {
        User user = new User();
        user.setEmail("anon@example.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcomeEmail(user);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseFeaturedPlan() {
        Plan featured = buildPlan("Featured Pro", "MONTHLY", 30, "Feature A|Feature B|Feature C|Feature D", true);
        Plan basic = buildPlan("Basic", "WEEKLY", 7, "Basic feature", false);

        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(basic, featured));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser);

        verify(planRepository).findByIsActiveTrueOrderByDurationDaysAsc();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseLastActivePlanWhenNoFeatured() {
        Plan basic = buildPlan("Basic", "WEEKLY", 7, "Basic feature", false);
        Plan premium = buildPlan("Premium", "MONTHLY", 30, "Premium feature", false);

        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(basic, premium));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseFallbackPlanWhenRepositoryEmpty() {
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of());
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseDefaultFeaturesWhenDescriptionBlank() {
        Plan plan = buildPlan("Premium", "LONGTERM", 90, "   ", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldParseMultilineDescription() {
        Plan plan = buildPlan("Annual", "MONTHLY", 365, "Line one\nLine two;Line three", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail(null);

        emailService.sendProfileCompletedEmail(user);

        verifyNoInteractions(planRepository);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldSendForSucceededPayment() {
        Subscription subscription = buildSubscription("MONTHLY", 30);
        Payment payment = buildPayment("succeeded", "pi_123", null, "Visa");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(validUser, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldSkipWhenPaymentNotSucceeded() {
        Subscription subscription = buildSubscription("WEEKLY", 7);
        Payment payment = buildPayment("failed", "pi_123", null, null);

        emailService.sendSubscriptionConfirmationEmail(validUser, subscription, payment);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldUseChargeIdWhenIntentMissing() {
        Subscription subscription = buildSubscription(null, 14);
        Payment payment = buildPayment("succeeded", null, "ch_456", null);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(validUser, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldUseFallbackTransactionId() {
        Subscription subscription = buildSubscription("LONGTERM", 90);
        Payment payment = buildPayment("succeeded", "", "", "PayPal");
        payment.setId(99L);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(validUser, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldFormatNonUsdCurrency() {
        Subscription subscription = buildSubscription("MONTHLY", 30);
        Payment payment = buildPayment("succeeded", "pi_eur", null, "Card");
        payment.setCurrency("eur");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(validUser, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundRequestReceivedEmailShouldSendWithPlanName() {
        Refund refund = buildRefund(42L);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRefundRequestReceivedEmail(validUser, refund, "Pro Monthly");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundRequestReceivedEmailShouldDefaultPlanNameWhenBlank() {
        Refund refund = buildRefund(7L);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRefundRequestReceivedEmail(validUser, refund, "  ");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendAdminRequestedCallEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAdminRequestedCallEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundApprovedEmailShouldSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRefundApprovedEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundFinalizedEmailShouldSend() {
        Refund refund = buildRefund(15L);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRefundFinalizedEmail(validUser, refund, new BigDecimal("95.00"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendForgotPasswordOtpEmailShouldSendSuccessfully() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendForgotPasswordOtpEmail(validUser, "123456");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendForgotPasswordOtpEmailShouldThrowWhenEmailInvalid() {
        User user = new User();
        user.setEmail("");

        assertThrows(MessagingException.class,
                () -> emailService.sendForgotPasswordOtpEmail(user, "123456"));
    }

    @Test
    void sendHtmlEmailShouldLogErrorWhenSendFails() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(validUser));
    }

    @Test
    void sendWelcomeEmailShouldSkipWhenEmailIsBlank() {
        User user = new User();
        user.setEmail("   ");

        emailService.sendWelcomeEmail(user);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private Plan buildPlan(String name, String type, int days, String description, boolean featured) {
        Plan plan = new Plan();
        plan.setPlanName(name);
        plan.setPlanType(type);
        plan.setDurationDays(days);
        plan.setAmount(BigDecimal.valueOf(19.99));
        plan.setDescription(description);
        plan.setFeatured(featured);
        return plan;
    }

    private Subscription buildSubscription(String planType, int durationDays) {
        Plan plan = buildPlan("Test Plan", planType, durationDays, "Feature 1|Feature 2", false);
        Subscription subscription = new Subscription();
        subscription.setPlan(plan);
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 12, 31, 0, 0));
        return subscription;
    }

    private Payment buildPayment(String status, String intentId, String chargeId, String method) {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setStatus(status);
        payment.setStripePaymentIntentId(intentId);
        payment.setStripeChargeId(chargeId);
        payment.setMethod(method);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        return payment;
    }

    private Refund buildRefund(Long refundId) {
        Payment payment = buildPayment("succeeded", "pi_ref", null, "Card");
        Refund refund = new Refund();
        refund.setRefundId(refundId);
        refund.setPayment(payment);
        refund.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return refund;
    }
}
