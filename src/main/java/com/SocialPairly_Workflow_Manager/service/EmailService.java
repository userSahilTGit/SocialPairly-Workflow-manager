package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Refund;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.util.EmailTemplateBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    private final JavaMailSender mailSender;
    private final PlanRepository planRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.merchant.name:SocialPairly}")
    private String appName;

    @Value("${app.support.email:sahil.t@socialpairly.com}")
    private String supportEmail;

    @Value("${app.email.base-url:https://socialpairly.com}")
    private String appBaseUrl;

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
        if (!hasValidEmail(user)) {
            return;
        }

        BigDecimal originalAmount = refund.getPayment().getAmount();
        BigDecimal deductedAmount = originalAmount.subtract(netRefundAmount).setScale(2, RoundingMode.HALF_UP);
        String userName = formatUserName(user);

        String summaryTable = EmailTemplateBuilder.refundSummaryTable(
                formatCurrency(originalAmount, refund.getPayment().getCurrency()),
                formatCurrency(deductedAmount, refund.getPayment().getCurrency()),
                formatCurrency(netRefundAmount, refund.getPayment().getCurrency()),
                "standard payment processing gateway fees (5% as per app policy)"
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
        mailSender.send(message);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to {} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} subject={}", to, subject, e);
        }
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
}
