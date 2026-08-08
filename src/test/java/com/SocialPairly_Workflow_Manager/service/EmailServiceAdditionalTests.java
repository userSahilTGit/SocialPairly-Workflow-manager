package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
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

import jakarta.mail.Session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceAdditionalTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PlanRepository planRepository;

    private EmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, planRepository);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "appName", "SocialPairly");
        ReflectionTestUtils.setField(emailService, "supportEmail", "support@test.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "https://test.com");

        Session session = Session.getDefaultInstance(new Properties());
        mimeMessage = new MimeMessage(session);
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldDefaultBlankPaymentMethodToCard() {
        User user = validUser();
        Subscription subscription = subscriptionWithPlan("MONTHLY", 30);
        Payment payment = new Payment();
        payment.setStatus("succeeded");
        payment.setStripePaymentIntentId("pi_blank_method");
        payment.setAmount(new BigDecimal("29.99"));
        payment.setCurrency("usd");
        payment.setMethod("   ");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldFilterEmptyFeatureTokens() {
        Plan plan = plan("Mixed", "MONTHLY", 30, "Real feature;;  ;Another", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldSkipWhenUserEmailInvalid() {
        User user = new User();
        user.setEmail("no-at-symbol");
        Subscription subscription = new Subscription();
        Payment payment = new Payment();
        payment.setStatus("succeeded");

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldDefaultPaymentMethodToCard() {
        User user = validUser();
        Subscription subscription = subscriptionWithPlan("WEEKLY", 7);
        Payment payment = new Payment();
        payment.setStatus("succeeded");
        payment.setStripePaymentIntentId("pi_weekly");
        payment.setAmount(new BigDecimal("9.99"));
        payment.setCurrency("usd");
        payment.setMethod(null);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundRequestReceivedEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail(null);
        Refund refund = new Refund();

        emailService.sendRefundRequestReceivedEmail(user, refund, "Pro");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundRequestReceivedEmailShouldDefaultNullPlanName() {
        User user = validUser();
        Refund refund = refundWithPayment();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRefundRequestReceivedEmail(user, refund, null);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendAdminRequestedCallEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail("invalid");

        emailService.sendAdminRequestedCallEmail(user);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundApprovedEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail("   ");

        emailService.sendRefundApprovedEmail(user);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRefundFinalizedEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail("bad");
        Refund refund = refundWithPayment();

        emailService.sendRefundFinalizedEmail(user, refund, new BigDecimal("50.00"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldPickLastFeaturedPlanWhenMultipleFeatured() {
        Plan firstFeatured = plan("Starter Featured", "MONTHLY", 30, "A|B|C", true);
        Plan secondFeatured = plan("Ultimate Featured", "MONTHLY", 60, "X|Y|Z", true);

        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc())
                .thenReturn(List.of(firstFeatured, secondFeatured));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseDefaultFeaturesWhenDescriptionIsOnlyDelimiters() {
        Plan plan = plan("Empty Desc", "MONTHLY", 30, ";;;", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseNullDescriptionDefaults() {
        Plan plan = plan("Null Desc", "MONTHLY", 30, null, false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseAnnualDiscountNoteForLongDurationNonLongtermPlan() {
        Plan plan = plan("Annual", "MONTHLY", 365, "Annual perks", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProfileCompletedEmailShouldUseFlexibleDiscountForShortLongtermPlan() {
        Plan plan = plan("Short Pass", "LONGTERM", 30, "Short perks", false);
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(plan));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendProfileCompletedEmail(validUser());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldFormatWeeklyBillingCycle() {
        User user = validUser();
        Subscription subscription = subscriptionWithPlan("WEEKLY", 7);
        Payment payment = new Payment();
        payment.setStatus("succeeded");
        payment.setStripePaymentIntentId("pi_weekly");
        payment.setAmount(new BigDecimal("9.99"));
        payment.setCurrency("usd");
        payment.setMethod("Card");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldFormatUnknownPlanTypeAsDays() {
        User user = validUser();
        Subscription subscription = subscriptionWithPlan("CUSTOM", 45);
        Payment payment = new Payment();
        payment.setStatus("succeeded");
        payment.setStripePaymentIntentId("pi_custom");
        payment.setAmount(new BigDecimal("45.00"));
        payment.setCurrency("usd");
        payment.setMethod("Card");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSubscriptionConfirmationEmailShouldUsePaymentIdWhenStripeIdsMissing() {
        User user = validUser();
        Subscription subscription = subscriptionWithPlan("MONTHLY", 30);
        Payment payment = new Payment();
        payment.setId(777L);
        payment.setStatus("succeeded");
        payment.setStripePaymentIntentId(null);
        payment.setStripeChargeId(null);
        payment.setAmount(new BigDecimal("29.99"));
        payment.setCurrency("usd");
        payment.setMethod("Card");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSubscriptionConfirmationEmail(user, subscription, payment);

        verify(mailSender).send(any(MimeMessage.class));
    }

    private User validUser() {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        return user;
    }

    private Plan plan(String name, String type, int days, String description, boolean featured) {
        Plan plan = new Plan();
        plan.setPlanName(name);
        plan.setPlanType(type);
        plan.setDurationDays(days);
        plan.setDescription(description);
        plan.setFeatured(featured);
        plan.setAmount(BigDecimal.TEN);
        return plan;
    }

    private Subscription subscriptionWithPlan(String planType, int durationDays) {
        Subscription subscription = new Subscription();
        subscription.setPlan(plan("Plan", planType, durationDays, "Feature", false));
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 12, 31, 0, 0));
        return subscription;
    }

    private Refund refundWithPayment() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("usd");
        Refund refund = new Refund();
        refund.setRefundId(1L);
        refund.setPayment(payment);
        refund.setCreatedAt(LocalDateTime.of(2026, 8, 1, 0, 0));
        return refund;
    }
}
