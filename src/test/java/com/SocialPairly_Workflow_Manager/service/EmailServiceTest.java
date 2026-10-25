package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.dto.SupportAttachment;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
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
    void sendAccountDeleteOtpEmailShouldSendSuccessfully() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAccountDeleteOtpEmail(validUser, "4321");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPlanPromotionEmailShouldDescribeActivePlans() throws MessagingException {
        Plan weekly = new Plan();
        weekly.setPlanName("Spark Weekly");
        weekly.setPlanType("WEEKLY");
        weekly.setDurationDays(7);
        weekly.setAmount(new BigDecimal("9.99"));
        weekly.setTokensIncluded("20");
        weekly.setDescription("Profile boosts|Photo sharing");
        when(planRepository.findByIsActiveTrueOrderByDurationDaysAsc()).thenReturn(List.of(weekly));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPlanPromotionEmail(validUser);

        verify(mailSender).send(any(MimeMessage.class));
        assertEquals("Choose a SocialPairly plan and start connecting", mimeMessage.getSubject());
    }

    @Test
    void sendPlanPromotionEmailShouldSkipWhenEmailInvalid() {
        User user = new User();
        user.setEmail("not-an-email");

        emailService.sendPlanPromotionEmail(user);

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendPasswordChangeOtpEmailShouldSendSuccessfully() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordChangeOtpEmail(validUser, "2468");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAccountDeleteOtpEmailShouldThrowWhenEmailInvalid() {
        User user = new User();
        user.setEmail(null);

        assertThrows(MessagingException.class,
                () -> emailService.sendAccountDeleteOtpEmail(user, "4321"));
    }

    @Test
    void sendHtmlEmailShouldLogErrorWhenSendFails() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(validUser));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmailShouldRetryTransientGmail421ThenSucceed() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailService, "initialBackoffMs", 1L);
        ReflectionTestUtils.setField(emailService, "minSendGapMs", 0L);
        doThrow(new MailSendException("421-4.3.0 Temporary System Problem. Try again later"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(validUser));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlEmailShouldNotRetryPermanentSmtpFailure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("550 5.1.1 The email account that you tried to reach does not exist"))
                .when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(validUser));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
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

    @Test
    void sendCustomerCareEmailOpensSharedInboxThread() throws Exception {
        MimeMessage agentMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMessage memberMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(agentMessage, memberMessage);

        emailService.sendCustomerCareEmail(new com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest(
                "Ada Lovelace",
                "ada@example.com",
                "+12025550123",
                List.of("Billing", "Technical"),
                "Checkout failed",
                "Unable to complete Premium upgrade.",
                "https://www.socialpairly.com/subscriptions"
        ));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
        assertEquals("support@test.com", agentMessage.getAllRecipients()[0].toString());
        assertTrue(agentMessage.getFrom()[0].toString().contains("support@test.com"));
        assertEquals("ada@example.com", agentMessage.getReplyTo()[0].toString());
        assertTrue(agentMessage.getSubject().contains("Checkout failed"));

        assertEquals("ada@example.com", memberMessage.getAllRecipients()[0].toString());
        assertTrue(memberMessage.getFrom()[0].toString().contains("support@test.com"));
        assertEquals("support@test.com", memberMessage.getReplyTo()[0].toString());
        assertTrue(memberMessage.getSubject().startsWith("Re:"));
    }

    @Test
    void sendCustomerCareEmailOmitsBlankPhoneAndPageAndHandlesNullIssues() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())),
                        new MimeMessage(Session.getDefaultInstance(new Properties())));

        emailService.sendCustomerCareEmail(new com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest(
                "Ada",
                "ada@example.com",
                "   ",
                null,
                "General help",
                "Need assistance with my account please.",
                null
        ));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendCustomerCareEmailTreatsBlankIssueTokensAsGeneral() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())),
                        new MimeMessage(Session.getDefaultInstance(new Properties())));

        emailService.sendCustomerCareEmail(new com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest(
                "Ada",
                "ada@example.com",
                null,
                java.util.Arrays.asList("  ", "", null),
                "Blank issues",
                "Message body with enough length.",
                "   "
        ));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendCustomerCareEmailHandlesEmptyIssuesList() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())),
                        new MimeMessage(Session.getDefaultInstance(new Properties())));

        emailService.sendCustomerCareEmail(new com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest(
                "Ada",
                "ada@example.com",
                null,
                List.of(),
                "Empty issues",
                "Message body with enough length.",
                ""
        ));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendCustomerCareEmailRendersMarkupAndAttachesFiles() throws Exception {
        MimeMessage agentMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMessage memberMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(agentMessage, memberMessage);

        emailService.sendCustomerCareEmail(new com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest(
                "Ada <script>",
                "ada@example.com",
                "+91-8973927367",
                List.of("Other"),
                "Checkout failed",
                "Line one\nLine two",
                "https://www.socialpairly.com/contact"
        ), List.of(new SupportAttachment("shot.png", "image/png", new byte[] {1, 2, 3})));

        String agentHtml = htmlOf(agentMessage);
        String memberHtml = htmlOf(memberMessage);

        assertFalse(agentHtml.contains("&lt;strong&gt;"));
        assertFalse(memberHtml.contains("&lt;strong&gt;"));
        assertTrue(memberHtml.contains("<strong>Checkout failed</strong>"));
        assertTrue(agentHtml.contains(">Message</"));
        assertTrue(agentHtml.contains("mailto:ada@example.com"));
        assertTrue(agentHtml.contains("tel:+918973927367"));
        assertTrue(agentHtml.contains("https://www.socialpairly.com/contact"));
        assertTrue(agentHtml.contains("Ada &lt;script&gt;"));
        assertFalse(agentHtml.contains("<script>"));
        assertTrue(agentHtml.contains("Line one<br/>Line two"));
        assertTrue(agentHtml.contains("Attachments: shot.png"));
        assertTrue(memberHtml.contains("Attachments: shot.png"));
        assertTrue(hasAttachment(agentMessage, "shot.png"));
        assertTrue(hasAttachment(memberMessage, "shot.png"));
    }

    private static String htmlOf(MimeMessage message) throws Exception {
        message.saveChanges();
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder html = new StringBuilder();
            collectText(multipart, html);
            return html.toString();
        }
        return String.valueOf(content);
    }

    private static void collectText(Multipart multipart, StringBuilder html) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            Object body = multipart.getBodyPart(i).getContent();
            if (body instanceof String text) {
                html.append(text);
            } else if (body instanceof Multipart nested) {
                collectText(nested, html);
            }
        }
    }

    private static boolean hasAttachment(MimeMessage message, String filename) throws Exception {
        Object content = message.getContent();
        if (!(content instanceof Multipart multipart)) {
            return false;
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (filename.equals(part.getFileName()) || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                return filename.equals(part.getFileName()) || part.getFileName() == null;
            }
        }
        return false;
    }
}
