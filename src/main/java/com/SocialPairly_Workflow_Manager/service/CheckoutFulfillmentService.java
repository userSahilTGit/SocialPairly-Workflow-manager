package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.CheckoutConfirmDto;
import com.SocialPairly_Workflow_Manager.dto.ReceiptDto;
import com.SocialPairly_Workflow_Manager.dto.SubscriptionDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFulfillmentService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.merchant.name:SocialPairly}")
    private String merchantName;

    @Value("${app.support.email:sahil.t@socialpairly.com}")
    private String supportEmail;

    public CheckoutFulfillmentService(SubscriptionRepository subscriptionRepository,
                                      PaymentRepository paymentRepository,
                                      UserRepository userRepository,
                                      PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public CheckoutConfirmDto fulfillCheckoutSession(String sessionId, Long expectedUserId) {
        Stripe.apiKey = secretKey;

        Session session;
        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException e) {
            log.error("Unable to retrieve Stripe checkout session {}", sessionId, e);
            throw new BadRequestException("Invalid checkout session");
        }

        return fulfillPaidSession(session, expectedUserId);
    }

    @Transactional
    public CheckoutConfirmDto fulfillPaidSession(Session session, Long expectedUserId) {
        if (!"paid".equals(session.getPaymentStatus())) {
            throw new BadRequestException("Checkout session is not paid. Status: " + session.getPaymentStatus());
        }

        Map<String, String> metadata = session.getMetadata();
        Long userId = parseLong(metadata.get("userId"), "userId");
        Long planId = parseLong(metadata.get("planId"), "planId");

        if (expectedUserId != null && !expectedUserId.equals(userId)) {
            throw new BadRequestException("Checkout session does not belong to the current user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(session.getId())
                .orElseGet(() -> createSubscription(session, user, plan));

        Payment payment = findOrCreatePayment(session, user, subscription);

        return new CheckoutConfirmDto(
                SubscriptionDto.from(subscription),
                buildReceiptDto(payment, plan, user),
                payment.getStatus()
        );
    }

    private ReceiptDto buildReceiptDto(Payment payment, Plan plan, User user) {
        String receiptNumber = fetchReceiptNumber(payment.getStripeChargeId());
        return new ReceiptDto(
                receiptNumber,
                plan.getPlanName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                user.getEmail(),
                payment.getReceiptUrl(),
                merchantName,
                supportEmail
        );
    }

    private String fetchReceiptNumber(String chargeId) {
        if (chargeId == null || chargeId.isBlank()) {
            return null;
        }
        Stripe.apiKey = secretKey;
        try {
            Charge charge = Charge.retrieve(chargeId);
            if (charge.getReceiptNumber() != null && !charge.getReceiptNumber().isBlank()) {
                return charge.getReceiptNumber();
            }
        } catch (StripeException e) {
            log.warn("Unable to retrieve receipt number for charge {}", chargeId, e);
        }
        return null;
    }

    private Subscription createSubscription(Session session, User user, Plan plan) {
        LocalDateTime periodStart = LocalDateTime.now();
        LocalDateTime periodEnd = periodStart.plusDays(plan.getDurationDays());

        String customerId = Optional.ofNullable(session.getCustomer())
                .filter(id -> !id.isBlank())
                .orElse("cus_checkout_" + session.getId());

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStripeCustomerId(customerId);
        subscription.setStripeSubscriptionId(session.getId());
        subscription.setStatus("active");
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setCancelAtPeriodEnd(false);

        subscription = subscriptionRepository.save(subscription);
        log.info("Created subscription id={} for userId={} planId={}", subscription.getId(), user.getId(), plan.getId());
        return subscription;
    }

    private Payment findOrCreatePayment(Session session, User user, Subscription subscription) {
        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId != null) {
            Optional<Payment> existing = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
            if (existing.isPresent()) {
                Payment payment = existing.get();
                if (payment.getSubscription() == null) {
                    payment.setSubscription(subscription);
                    payment = paymentRepository.save(payment);
                }
                return payment;
            }
        }

        Payment payment = buildPaymentFromSession(session, user, subscription);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void recordFailedPaymentIntent(PaymentIntent paymentIntent) {
        if (paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).isPresent()) {
            return;
        }

        Map<String, String> metadata = paymentIntent.getMetadata();
        if (metadata == null || metadata.get("userId") == null) {
            log.warn("PaymentIntent {} failed without user metadata", paymentIntent.getId());
            return;
        }

        Long userId = parseLong(metadata.get("userId"), "userId");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for webhook: " + userId));

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setStripeChargeId(paymentIntent.getLatestCharge());
        payment.setAmount(centsToDollars(paymentIntent.getAmount()));
        payment.setCurrency(paymentIntent.getCurrency());
        payment.setStatus("failed");
        paymentRepository.save(payment);

        log.info("Recorded failed payment for userId={} paymentIntent={}", userId, paymentIntent.getId());
    }

    private Payment buildPaymentFromSession(Session session, User user, Subscription subscription) {
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setSubscription(subscription);
        payment.setStripePaymentIntentId(session.getPaymentIntent());
        payment.setAmount(centsToDollars(session.getAmountTotal()));
        payment.setCurrency(session.getCurrency());
        payment.setStatus("succeeded");

        if (session.getPaymentIntent() != null) {
            Stripe.apiKey = secretKey;
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());
                payment.setStripeChargeId(paymentIntent.getLatestCharge());
                payment.setReceiptUrl(fetchReceiptUrl(paymentIntent.getLatestCharge()));
            } catch (StripeException e) {
                log.warn("Unable to retrieve payment intent {} for session {}",
                        session.getPaymentIntent(), session.getId(), e);
            }
        }

        return payment;
    }

    private String fetchReceiptUrl(String chargeId) {
        if (chargeId == null || chargeId.isBlank()) {
            return null;
        }
        Stripe.apiKey = secretKey;
        try {
            Charge charge = Charge.retrieve(chargeId);
            return charge.getReceiptUrl();
        } catch (StripeException e) {
            log.warn("Unable to retrieve charge {}", chargeId, e);
            return null;
        }
    }

    BigDecimal centsToDollars(Long amountInCents) {
        if (amountInCents == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(amountInCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private Long parseLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Missing Stripe metadata field: " + fieldName);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid Stripe metadata for " + fieldName + ": " + value);
        }
    }
}
