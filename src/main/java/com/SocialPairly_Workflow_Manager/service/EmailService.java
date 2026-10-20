package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.EventDetail;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.dto.CustomerCareRequest;
import com.SocialPairly_Workflow_Manager.dto.RefundTokenAdjustment;
import com.SocialPairly_Workflow_Manager.dto.SupportAttachment;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder;
import com.SocialPairly_Workflow_Manager.util.LogMasker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter EVENT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final PlanRepository planRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.merchant.name:SocialPairly}")
    private String appName;

    @Value("${app.support.email:customercare@socialpairly.com}")
    private String supportEmail;

    @Value("${app.email.base-url:https://www.socialpairly.com}")
    private String appBaseUrl;

    @Value("${app.frontend.url:http://localhost:3002}")
    private String frontendUrl;

    /** Only one SMTP dialog at a time. Gmail answers 421 when many sessions open together. */
    private final Object smtpSendLock = new Object();
    private long lastSmtpSendAtMillis;

    @Value("${app.mail.send-gap-ms:400}")
    private long minSendGapMs;

    @Value("${app.mail.retry.max-attempts:4}")
    private int maxSendAttempts = 4;

    @Value("${app.mail.retry.initial-backoff-ms:2000}")
    private long initialBackoffMs = 2000L;

    public EmailService(JavaMailSender mailSender, PlanRepository planRepository) {
        this.mailSender = mailSender;
        this.planRepository = planRepository;
    }

    @Async
    public void sendWelcomeEmail(User user) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String profileUrl = appBaseUrl + "/profile";

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph("Welcome to " + appName + "! We're thrilled to have you on board.")
                + EmailTemplateBuilder.paragraph(
                "To get the absolute best out of your experience, please take a minute to complete your profile. "
                        + "A complete profile helps us personalize your dashboard and unlock all core features.")
                + EmailTemplateBuilder.button("Complete Your Profile Now", profileUrl)
                + EmailTemplateBuilder.paragraph(
                "If you have any questions, feel free to reply directly to this email. We're here to help!")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Welcome to " + appName + "! Let's get you set up",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendEventInvitationEmail(User user, EventDetail event) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String dashboardUrl = frontendUrl + "/";
        String eventDateTime = event.getEventDate().atTime(event.getEventTime()).format(EVENT_DATETIME_FORMAT);
        String locationLine = event.getVenueName() + ", " + event.getLocation();

        String eventDetails = EmailTemplateBuilder.infoBox("Event Details", List.of(
                "Event: " + event.getTitle(),
                "Date & Time: " + eventDateTime,
                "Location: " + locationLine,
                "Event Code: " + event.getEventCode()
        ));

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "You've been selected to join an exclusive upcoming event: " + event.getTitle() + "!")
                + eventDetails
                + EmailTemplateBuilder.paragraph(
                "Log in to your account now to view full details, accept your invitation, and connect with other attendees!")
                + EmailTemplateBuilder.button("View Invitation on Dashboard", dashboardUrl)
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "You're Invited! Exclusive access to " + event.getTitle() + " 🎟️",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendEventUpdateEmail(User user, EventDetail event) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String dashboardUrl = frontendUrl + "/";
        String eventDateTime = event.getEventDate().atTime(event.getEventTime()).format(EVENT_DATETIME_FORMAT);
        String locationLine = event.getVenueName() + ", " + event.getLocation();

        String eventDetails = EmailTemplateBuilder.infoBox("Updated Event Details", List.of(
                "Event: " + event.getTitle(),
                "Date & Time: " + eventDateTime,
                "Location: " + locationLine,
                "Event Code: " + event.getEventCode()
        ));

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "Important update: details for " + event.getTitle() + " have recently changed.")
                + eventDetails
                + EmailTemplateBuilder.paragraph(
                "Please log in to your dashboard to review the latest event information and confirm your plans.")
                + EmailTemplateBuilder.button("View Updated Event on Dashboard", dashboardUrl)
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Update: " + event.getTitle() + " details have changed 📢",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendInvitationAcceptedEmail(User user, EventDetail event, String entryCode) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String dashboardUrl = frontendUrl + "/events/" + event.getId();
        String eventDate = event.getEventDate().format(DATE_FORMAT);
        String locationLine = event.getVenueName() + ", " + event.getLocation();

        String passCodeBox = EmailTemplateBuilder.highlightBox(
                "<p style=\"margin:0 0 8px;font-size:14px;font-weight:600;color:#6366f1;\">Your Pass Code</p>"
                        + "<p style=\"margin:0;font-size:22px;font-weight:800;color:#111827;letter-spacing:1px;\">"
                        + entryCode + "</p>"
        );

        String summary = EmailTemplateBuilder.infoBox("Quick Event Summary", List.of(
                "Event Name: " + event.getTitle(),
                "Date: " + eventDate,
                "Venue: " + locationLine
        ));

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph("Thanks for confirming your presence at " + event.getTitle() + "!")
                + EmailTemplateBuilder.paragraph(
                "Please save this code or keep this email handy. You will need to show this code to the event admin at entry for verification:")
                + passCodeBox
                + summary
                + EmailTemplateBuilder.paragraph(
                "You can now log into your dashboard to browse profiles of other participants and interact before the event begins!")
                + EmailTemplateBuilder.button("Open Event Dashboard", dashboardUrl)
                + EmailTemplateBuilder.paragraph("See you there!")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "You're In! Here is your entry pass for " + event.getTitle() + " 🎫",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendProfileReactionEmail(User recipient, User reactingUser, EventDetail event, ReactionType reactionType) {
        if (!hasValidEmail(recipient)) {
            return;
        }

        String recipientName = formatUserName(recipient);
        String reactingName = formatUserName(reactingUser);
        String dashboardUrl = frontendUrl + "/events/" + event.getId();
        String reactionLabel = formatReactionLabel(reactionType);

        String body = EmailTemplateBuilder.paragraph("Hi " + recipientName + ",")
                + EmailTemplateBuilder.paragraph(
                reactingName + " just reacted with a " + reactionLabel + " on your profile inside the "
                        + event.getTitle() + " event workspace!")
                + EmailTemplateBuilder.paragraph(
                "Want to see who's attending and react back? Head over to your dashboard now.")
                + EmailTemplateBuilder.button("View Profile & React Back", dashboardUrl)
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                recipient.getEmail(),
                "Someone noticed you on " + event.getTitle() + "! 👀",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendProfileCompletedEmail(User user) {
        if (!hasValidEmail(user)) {
            return;
        }

        Plan trendingPlan = findTrendingPlan();
        String userName = formatUserName(user);
        String subscriptionsUrl = appBaseUrl + "/subscriptions";
        List<String> features = parsePlanFeatures(trendingPlan);
        String discountNote = buildDiscountNote(trendingPlan);

        String trendingSection = EmailTemplateBuilder.highlightBox(
                "<p style=\"margin:0 0 8px;font-size:16px;font-weight:600;color:#6366f1;\">"
                        + "Trending Right Now: The " + trendingPlan.getPlanName() + " Plan</p>"
                        + "<p style=\"margin:0 0 8px;font-size:14px;color:#374151;\">Our most popular choice among users includes:</p>"
                        + EmailTemplateBuilder.featureList(features)
                        + "<p style=\"margin:0;font-size:14px;color:#059669;font-weight:600;\">" + discountNote + "</p>"
        );

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph("Thanks for completing your profile! You're all set to experience everything "
                + appName + " has to offer.")
                + EmailTemplateBuilder.paragraph(
                "Ready to supercharge your results? Upgrading to a premium plan unlocks exclusive tools, "
                        + "advanced features, and priority support tailored just for you.")
                + trendingSection
                + EmailTemplateBuilder.button("Explore Plans & Upgrade", subscriptionsUrl)
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Profile complete! Ready to unlock " + appName + "'s full potential?",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendSubscriptionConfirmationEmail(User user, Subscription subscription, Payment payment) {
        if (!hasValidEmail(user) || !"succeeded".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        Plan plan = subscription.getPlan();
        String userName = formatUserName(user);
        String dashboardUrl = appBaseUrl + "/subscriptions";
        String billingCycle = formatBillingCycle(plan);
        String renewalDate = subscription.getCurrentPeriodEnd().format(DATE_FORMAT);
        String transactionId = resolveTransactionId(payment);
        String amountPaid = formatCurrency(payment.getAmount(), payment.getCurrency());
        String paymentMethod = payment.getMethod() != null && !payment.getMethod().isBlank()
                ? payment.getMethod()
                : "Card";

        String subscriptionDetails = EmailTemplateBuilder.infoBox("Subscription Details", List.of(
                "Plan Name: " + plan.getPlanName(),
                "Billing Cycle: " + billingCycle,
                "Renewal Date: " + renewalDate
        ));

        String paymentReceipt = EmailTemplateBuilder.infoBox("Payment Receipt", List.of(
                "Transaction ID: " + transactionId,
                "Amount Paid: " + amountPaid,
                "Payment Method: " + paymentMethod
        ));

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "Thank you for upgrading! Your payment was successful, and your premium subscription is now active.")
                + subscriptionDetails
                + paymentReceipt
                + EmailTemplateBuilder.paragraph(
                "You can manage your subscription settings and download official invoices anytime directly from your dashboard account.")
                + EmailTemplateBuilder.button("Go to My Dashboard", dashboardUrl)
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Subscription Confirmed! Welcome to " + plan.getPlanName(),
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendRefundRequestReceivedEmail(User user, Refund refund, String planName) {
        if (!hasValidEmail(user)) {
            return;
        }

        if (planName == null || planName.isBlank()) {
            planName = "subscription";
        }
        String userName = formatUserName(user);
        String ticketId = refund.getFormattedRefundId();
        String submittedDate = refund.getCreatedAt().format(DATE_FORMAT);

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "We have received your request for a refund regarding your " + planName + " subscription.")
                + EmailTemplateBuilder.paragraph(
                "Our support team is currently reviewing your request against our policy guidelines. "
                        + "We take every request seriously and will get back to you within 24-48 hours with an update.")
                + EmailTemplateBuilder.infoBox("Request Details", List.of(
                "Request ID: #" + ticketId,
                "Date Submitted: " + submittedDate
        ))
                + EmailTemplateBuilder.paragraph("Thank you for your patience while we look into this for you.")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph(appName + " Support Team");

        sendHtmlEmail(
                user.getEmail(),
                "We've received your refund request [Ticket #" + ticketId + "]",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendAdminRequestedCallEmail(User user) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String slotUrl = appBaseUrl + "/subscriptions";

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "Regarding your recent query/refund request, our support team would like to schedule a quick call with you "
                        + "to better assist you and resolve the issue smoothly.")
                + EmailTemplateBuilder.paragraph(
                "Please log into the app and choose a time slot that works best for your schedule.")
                + EmailTemplateBuilder.button("Select Your Call Slot", slotUrl)
                + EmailTemplateBuilder.paragraph("Looking forward to speaking with you!")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph(appName + " Support Team");

        sendHtmlEmail(
                user.getEmail(),
                "Action Required: Select a time slot for your support call",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendRefundApprovedEmail(User user) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String bankDetailsUrl = appBaseUrl + "/subscriptions";

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph("Good news! Your refund request has been approved by our admin team.")
                + EmailTemplateBuilder.paragraph(
                "To proceed with processing your refund, please log in to the " + appName
                        + " app and submit your bank account details under your billing settings.")
                + EmailTemplateBuilder.button("Provide Bank Account Details", bankDetailsUrl)
                + EmailTemplateBuilder.paragraph(
                "Note: Please ensure all account details are entered correctly to avoid any processing delays.")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph(appName + " Support Team");

        sendHtmlEmail(
                user.getEmail(),
                "Refund Approved – Please update your payout details",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    @Async
    public void sendRefundFinalizedEmail(User user, Refund refund, BigDecimal netRefundAmount) {
        sendRefundFinalizedEmail(user, refund, netRefundAmount, null);
    }

    @Async
    public void sendRefundFinalizedEmail(User user,
                                         Refund refund,
                                         BigDecimal netRefundAmount,
                                         RefundTokenAdjustment tokenAdjustment) {
        if (!hasValidEmail(user)) {
            return;
        }

        BigDecimal originalAmount = refund.getPayment().getAmount();
        BigDecimal processingFee = originalAmount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        String currency = refund.getPayment().getCurrency();
        String userName = formatUserName(user);

        boolean hasTokenDeduction = tokenAdjustment != null && tokenAdjustment.hasTokenUsageDeduction();
        String tokenUsageFormatted = hasTokenDeduction
                ? formatCurrency(tokenAdjustment.tokenUsageCost(), currency)
                : null;

        String deductionNote;
        if (hasTokenDeduction) {
            deductionNote = "As per our terms of service, a processing fee of "
                    + formatCurrency(processingFee, currency)
                    + " (5% as per app policy) was applied. Additionally, "
                    + tokenAdjustment.excessTokensUsed()
                    + " plan token"
                    + (tokenAdjustment.excessTokensUsed() == 1 ? "" : "s")
                    + " had already been used, so "
                    + tokenUsageFormatted
                    + " was deducted from your refund for that usage. Your token balance has been set to 0.";
        } else {
            deductionNote = "As per our terms of service, a deduction of "
                    + formatCurrency(processingFee, currency)
                    + " was applied for standard payment processing gateway fees (5% as per app policy).";
            if (tokenAdjustment != null && tokenAdjustment.tokensRemoved() > 0) {
                deductionNote += " The "
                        + tokenAdjustment.tokensRemoved()
                        + " token"
                        + (tokenAdjustment.tokensRemoved() == 1 ? "" : "s")
                        + " included with your plan have also been removed from your available balance.";
            }
        }

        String summaryTable = EmailTemplateBuilder.refundSummaryTable(
                formatCurrency(originalAmount, currency),
                formatCurrency(processingFee, currency),
                tokenUsageFormatted,
                formatCurrency(netRefundAmount, currency),
                deductionNote
        );

        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph("Your refund has been successfully processed and sent to your bank account.")
                + summaryTable
                + EmailTemplateBuilder.paragraph(
                "Depending on your financial institution, it may take 3–5 business days for the funds to reflect in your account.")
                + EmailTemplateBuilder.paragraph(
                "If you have any further questions, please feel free to reach back out to us.")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Refund Processed – " + appName,
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    public void sendForgotPasswordOtpEmail(User user, String otp) throws MessagingException {
        if (!hasValidEmail(user)) {
            throw new MessagingException("User does not have a valid email address");
        }

        String userName = formatUserName(user);
        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "We received a request to reset your account password. Please use the verification One-Time Password (OTP) below to complete your process:")
                + """
                <div style="text-align:center;margin:24px 0;">
                  <span style="font-size:32px;font-weight:bold;letter-spacing:5px;color:#6366f1;background-color:#eef2ff;padding:12px 32px;border-radius:8px;border:2px dashed #8b5cf6;display:inline-block;">%s</span>
                </div>
                """.formatted(otp)
                + EmailTemplateBuilder.paragraph("This OTP is valid for the next 10 minutes.")
                + EmailTemplateBuilder.paragraph(
                "If you did not make this request, you can safely ignore this email. Your password will remain unchanged.");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(user.getEmail());
        helper.setSubject("Password Reset Verification Code");
        helper.setText(EmailTemplateBuilder.build(appName, supportEmail, body), true);
        deliver(message);
    }

    public void sendEmailVerificationOtpEmail(User user, String otp) throws MessagingException {
        if (!hasValidEmail(user)) {
            throw new MessagingException("User does not have a valid email address");
        }

        String userName = formatUserName(user);
        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "Welcome to " + appName + "! Please use the verification One-Time Password (OTP) below to verify your email:")
                + """
                <div style="text-align:center;margin:24px 0;">
                  <span style="font-size:32px;font-weight:bold;letter-spacing:5px;color:#6366f1;background-color:#eef2ff;padding:12px 32px;border-radius:8px;border:2px dashed #8b5cf6;display:inline-block;">%s</span>
                </div>
                """.formatted(otp)
                + EmailTemplateBuilder.paragraph("This OTP is valid for the next 10 minutes.")
                + EmailTemplateBuilder.paragraph(
                "If you did not create an account, you can safely ignore this email.");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(user.getEmail());
        helper.setSubject("Verify your Socialpairly email");
        helper.setText(EmailTemplateBuilder.build(appName, supportEmail, body), true);
        deliver(message);
    }

    public void sendAccountDeleteOtpEmail(User user, String otp) throws MessagingException {
        if (!hasValidEmail(user)) {
            throw new MessagingException("User does not have a valid email address");
        }

        String userName = formatUserName(user);
        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "We received a request to permanently delete your " + appName + " account. "
                        + "Use the One-Time Password (OTP) below to confirm:")
                + """
                <div style="text-align:center;margin:24px 0;">
                  <span style="font-size:32px;font-weight:bold;letter-spacing:5px;color:#e11d48;background-color:#fff1f2;padding:12px 32px;border-radius:8px;border:2px dashed #fb7185;display:inline-block;">%s</span>
                </div>
                """.formatted(otp)
                + EmailTemplateBuilder.paragraph("This OTP is valid for the next 5 minutes.")
                + EmailTemplateBuilder.paragraph(
                "If you did not request account deletion, you can safely ignore this email. Your account will remain active.");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);
        helper.setTo(user.getEmail());
        helper.setSubject("Confirm account deletion");
        helper.setText(EmailTemplateBuilder.build(appName, supportEmail, body), true);
        deliver(message);
    }

    /**
     * Shared-inbox support channel (not noreply):
     * <ul>
     *   <li>Agent inbox ({@code supportEmail}) receives the ticket; Reply goes to the member.</li>
     *   <li>Member receives an acknowledgement from {@code supportEmail} so further replies
     *       stay on the same customercare thread.</li>
     * </ul>
     * OTP / system mail continues to use {@code senderEmail} (noreply).
     */
    public void sendCustomerCareEmail(CustomerCareRequest request) throws MessagingException {
        sendCustomerCareEmail(request, List.of());
    }

    public void sendCustomerCareEmail(CustomerCareRequest request, List<SupportAttachment> attachments)
            throws MessagingException {
        String customerName = request.name().trim();
        String customerEmail = request.email().trim();
        String issuesLabel = formatIssues(request.issues());
        String subjectLine = "[Customer Care] " + request.subject().trim();
        String messageText = request.message().trim();
        List<SupportAttachment> files = attachments == null ? List.of() : attachments;
        String attachmentLabel = formatAttachmentNames(files);

        List<String> detailLines = new ArrayList<>();
        detailLines.add(EmailTemplateBuilder.plainToHtml("From: " + customerName));
        detailLines.add("Email: " + EmailTemplateBuilder.textLink("mailto:" + customerEmail, customerEmail));
        if (request.phone() != null && !request.phone().isBlank()) {
            String phone = request.phone().trim();
            detailLines.add("Phone: " + EmailTemplateBuilder.textLink(telHref(phone), phone));
        }
        detailLines.add(EmailTemplateBuilder.plainToHtml("Issues: " + issuesLabel));
        if (request.pageUrl() != null && !request.pageUrl().isBlank()) {
            String page = request.pageUrl().trim();
            if (isHttpUrl(page)) {
                detailLines.add("Page: " + EmailTemplateBuilder.textLink(page, page));
            } else {
                detailLines.add(EmailTemplateBuilder.plainToHtml("Page: " + page));
            }
        }
        if (!attachmentLabel.isBlank()) {
            detailLines.add(EmailTemplateBuilder.plainToHtml("Attachments: " + attachmentLabel));
        }

        String agentBody = EmailTemplateBuilder.paragraph(
                "A member contacted " + appName + " customer care. Reply to this email to continue the conversation with them.")
                + EmailTemplateBuilder.htmlInfoBox("Contact details", detailLines)
                + EmailTemplateBuilder.labeledBlock("Message", messageText);

        MimeMessage agentMessage = mailSender.createMimeMessage();
        MimeMessageHelper agentHelper = new MimeMessageHelper(agentMessage, true, "UTF-8");
        agentHelper.setFrom(supportFromAddress(customerName));
        agentHelper.setTo(supportEmail);
        agentHelper.setReplyTo(customerEmail);
        agentHelper.setSubject(subjectLine);
        agentHelper.setText(EmailTemplateBuilder.build(appName, supportEmail, agentBody), true);
        addSupportAttachments(agentHelper, files);
        deliver(agentMessage);

        List<String> memberLines = new ArrayList<>();
        memberLines.add(EmailTemplateBuilder.plainToHtml("Issues: " + issuesLabel));
        memberLines.add(EmailTemplateBuilder.plainToHtml(messageText));
        if (!attachmentLabel.isBlank()) {
            memberLines.add(EmailTemplateBuilder.plainToHtml("Attachments: " + attachmentLabel));
        }

        String memberBody = EmailTemplateBuilder.paragraph("Hi " + customerName + ",")
                + EmailTemplateBuilder.htmlParagraph(
                "We received your message about <strong>"
                        + EmailTemplateBuilder.plainToHtml(request.subject().trim())
                        + "</strong>.")
                + EmailTemplateBuilder.paragraph(
                "Our customer care team will reply on this email thread. You can reply directly to this message to continue the conversation.")
                + EmailTemplateBuilder.htmlInfoBox("Your message", memberLines);

        MimeMessage memberMessage = mailSender.createMimeMessage();
        MimeMessageHelper memberHelper = new MimeMessageHelper(memberMessage, true, "UTF-8");
        memberHelper.setFrom(supportFromAddress(appName + " Customer Care"));
        memberHelper.setTo(customerEmail);
        memberHelper.setReplyTo(supportEmail);
        memberHelper.setSubject("Re: " + subjectLine);
        memberHelper.setText(EmailTemplateBuilder.build(appName, supportEmail, memberBody), true);
        addSupportAttachments(memberHelper, files);
        deliver(memberMessage);

        log.info("Customer-care thread opened support={} member={} attachments={}",
                LogMasker.email(supportEmail), LogMasker.email(customerEmail), files.size());
    }

    private static void addSupportAttachments(MimeMessageHelper helper, List<SupportAttachment> attachments)
            throws MessagingException {
        int index = 1;
        for (SupportAttachment attachment : attachments) {
            if (attachment == null || attachment.data() == null || attachment.data().length == 0) {
                continue;
            }
            String filename = attachment.filename() == null || attachment.filename().isBlank()
                    ? "attachment-" + index
                    : attachment.filename().trim();
            String contentType = attachment.contentType() == null || attachment.contentType().isBlank()
                    ? "application/octet-stream"
                    : attachment.contentType();
            helper.addAttachment(filename, new ByteArrayResource(attachment.data()), contentType);
            index++;
        }
    }

    private static String formatAttachmentNames(List<SupportAttachment> attachments) {
        List<String> names = new ArrayList<>();
        int index = 1;
        for (SupportAttachment attachment : attachments) {
            if (attachment == null || attachment.data() == null || attachment.data().length == 0) {
                continue;
            }
            String filename = attachment.filename() == null || attachment.filename().isBlank()
                    ? "attachment-" + index
                    : attachment.filename().trim();
            names.add(filename);
            index++;
        }
        return String.join(", ", names);
    }

    private static String telHref(String phone) {
        StringBuilder href = new StringBuilder("tel:");
        for (int i = 0; i < phone.length(); i++) {
            char c = phone.charAt(i);
            if ((c == '+' && href.length() == 4) || Character.isDigit(c)) {
                href.append(c);
            }
        }
        return href.toString();
    }

    private static boolean isHttpUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    /** Visible From for the support channel (customercare), not noreply. */
    private InternetAddress supportFromAddress(String personalName) throws MessagingException {
        try {
            return new InternetAddress(supportEmail, personalName == null || personalName.isBlank()
                    ? appName + " Customer Care"
                    : personalName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new MessagingException("Unable to build support From address", ex);
        }
    }

    private static String formatIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "General / all issues";
        }
        List<String> cleaned = issues.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return cleaned.isEmpty() ? "General / all issues" : String.join(", ", cleaned);
    }

    @Async
    public void sendAccountDisabledEmail(User user) {
        if (!hasValidEmail(user)) {
            return;
        }

        String userName = formatUserName(user);
        String body = EmailTemplateBuilder.paragraph("Hi " + userName + ",")
                + EmailTemplateBuilder.paragraph(
                "Your " + appName + " account has been disabled by an administrator after repeated unsuccessful "
                        + "sign-in attempts that triggered our account protection rules.")
                + EmailTemplateBuilder.paragraph(
                "If you believe this was a mistake, or you need help restoring access, please contact our "
                        + "Administrator at " + supportEmail + ".")
                + EmailTemplateBuilder.paragraph("Best regards,")
                + EmailTemplateBuilder.paragraph("The " + appName + " Team");

        sendHtmlEmail(
                user.getEmail(),
                "Your " + appName + " account has been disabled",
                EmailTemplateBuilder.build(appName, supportEmail, body)
        );
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            deliver(message);
            log.info("Email sent successfully to {} subject={}", LogMasker.email(to), subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} subject={}", LogMasker.email(to), subject, e);
        }
    }

    /**
     * Gmail SMTP returns 421 4.3.0 when several messages are submitted at once.
     * Event fan-out runs each recipient on the async pool, so sends are serialized
     * and temporary 4xx responses are retried. Permanent failures still fail immediately.
     */
    private void deliver(MimeMessage message) {
        synchronized (smtpSendLock) {
            int attempts = Math.max(1, maxSendAttempts);
            for (int attempt = 1; attempt <= attempts; attempt++) {
                waitForSendGap();
                try {
                    mailSender.send(message);
                    lastSmtpSendAtMillis = System.currentTimeMillis();
                    return;
                } catch (MailException ex) {
                    if (!isTransientSmtpFailure(ex) || attempt == attempts) {
                        throw ex;
                    }
                    long backoff = initialBackoffMs * (1L << (attempt - 1));
                    log.warn("Transient SMTP failure (attempt {}/{}). Retrying in {} ms. {}",
                            attempt, attempts, backoff, ex.getMessage());
                    sleepForMail(backoff);
                }
            }
        }
    }

    private void waitForSendGap() {
        if (minSendGapMs <= 0 || lastSmtpSendAtMillis <= 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - lastSmtpSendAtMillis;
        if (elapsed < minSendGapMs) {
            sleepForMail(minSendGapMs - elapsed);
        }
    }

    private static void sleepForMail(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MailSendException("Interrupted while waiting to send email", ex);
        }
    }

    private static boolean isTransientSmtpFailure(Throwable error) {
        if (error instanceof MailSendException mailSend && mailSend.getFailedMessages() != null) {
            for (Exception failed : mailSend.getFailedMessages().values()) {
                if (hasTransientSmtpSignal(failed)) {
                    return true;
                }
            }
        }
        return hasTransientSmtpSignal(error);
    }

    private static boolean hasTransientSmtpSignal(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SMTPSendFailedException smtp) {
                int code = smtp.getReturnCode();
                if (code >= 400 && code < 500) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && looksLikeTransientSmtp(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean looksLikeTransientSmtp(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("temporary system problem")
                || lower.contains("try again later")
                || lower.contains("4.3.0")
                || lower.contains("4.7.0")
                || lower.contains("4.7.1")
                || lower.contains("rate limit")
                || lower.contains("too many")
                || lower.contains("resources temporarily unavailable")
                || lower.contains("421")
                || lower.contains("450 ")
                || lower.contains("451 ")
                || lower.contains("452 ");
    }

    private Plan findTrendingPlan() {
        List<Plan> activePlans = planRepository.findByIsActiveTrueOrderByDurationDaysAsc();
        Optional<Plan> featured = activePlans.stream()
                .filter(Plan::isFeatured)
                .reduce((first, second) -> second);
        if (featured.isPresent()) {
            return featured.get();
        }
        if (!activePlans.isEmpty()) {
            return activePlans.get(activePlans.size() - 1);
        }
        Plan fallback = new Plan();
        fallback.setPlanName("Premium");
        fallback.setPlanType("MONTHLY");
        fallback.setDurationDays(30);
        fallback.setAmount(BigDecimal.valueOf(29.99));
        fallback.setDescription("Full access to advanced analytics|Unlimited usage & priority processing|Dedicated 24/7 support");
        return fallback;
    }

    private List<String> parsePlanFeatures(Plan plan) {
        if (plan.getDescription() == null || plan.getDescription().isBlank()) {
            return List.of(
                    "Full access to advanced analytics/tools",
                    "Unlimited usage & priority processing",
                    "Dedicated 24/7 support"
            );
        }
        List<String> features = new ArrayList<>();
        Arrays.stream(plan.getDescription().split("[\\n|;•]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(features::add);

        if (features.isEmpty()) {
            return List.of(
                    "Full access to advanced analytics/tools",
                    "Unlimited usage & priority processing",
                    "Dedicated 24/7 support"
            );
        }
        return features.size() > 3 ? features.subList(0, 3) : features;
    }

    private String buildDiscountNote(Plan plan) {
        if ("LONGTERM".equalsIgnoreCase(plan.getPlanType()) && plan.getDurationDays() >= 90) {
            return "Save more with longer-term plans — best value for dedicated users!";
        }
        if (plan.getDurationDays() >= 365) {
            return "Save up to 20% with an annual subscription!";
        }
        return "Flexible plans available — upgrade anytime from your dashboard.";
    }

    private String formatBillingCycle(Plan plan) {
        if (plan.getPlanType() == null) {
            return plan.getDurationDays() + " days";
        }
        return switch (plan.getPlanType().toUpperCase(Locale.ROOT)) {
            case "WEEKLY" -> "Weekly";
            case "MONTHLY" -> "Monthly";
            case "LONGTERM" -> plan.getDurationDays() + "-Day Pass";
            default -> plan.getDurationDays() + " days";
        };
    }

    private String resolveTransactionId(Payment payment) {
        if (payment.getStripePaymentIntentId() != null && !payment.getStripePaymentIntentId().isBlank()) {
            return payment.getStripePaymentIntentId();
        }
        if (payment.getStripeChargeId() != null && !payment.getStripeChargeId().isBlank()) {
            return payment.getStripeChargeId();
        }
        return "PAY-" + payment.getId();
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        String symbol = "usd".equalsIgnoreCase(currency) ? "$" : currency.toUpperCase(Locale.ROOT) + " ";
        return symbol + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatUserName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "there" : fullName;
    }

    private boolean hasValidEmail(User user) {
        return user.getEmail() != null
                && user.getEmail().contains("@")
                && !user.getEmail().isBlank();
    }

    private String formatReactionLabel(ReactionType reactionType) {
        return switch (reactionType) {
            case Wave -> "👋 Wave";
            case Spark -> "⭐ Spark";
            case Heart -> "❤️ Heart";
            case Coffee -> "☕ Coffee";
            case Priority -> "💎 Priority";
        };
    }
}
