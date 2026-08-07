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
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
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

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
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

        Subscription subscription = findOrCreateSubscription(session, user, plan);

        Payment payment = findOrCreatePayment(session, user, subscription);

        return new CheckoutConfirmDto(
                SubscriptionDto.from(subscription),
                buildReceiptDto(payment, plan, user),
                payment.getStatus()
        );
    }

    private ReceiptDto buildReceiptDto(Payment payment, Plan plan, User user) {
        String chargeId = payment.getStripeChargeId();
        String receiptNumber = fetchReceiptNumber(chargeId);
        String paymentMethod = resolveStoredOrFetchPaymentMethod(payment);
        return new ReceiptDto(
                receiptNumber,
                plan.getPlanName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                user.getEmail(),
                payment.getReceiptUrl(),
                merchantName,
                supportEmail,
                paymentMethod
        );
    }

    private String resolveStoredOrFetchPaymentMethod(Payment payment) {
        if (payment.getMethod() != null && !payment.getMethod().isBlank()) {
            return payment.getMethod();
        }
        return fetchPaymentMethodLabel(payment.getStripePaymentIntentId(), payment.getStripeChargeId());
    }

    private String fetchReceiptNumber(String chargeId) {
        if (chargeId == null || chargeId.isBlank() || !chargeId.startsWith("ch_")) {
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

    private String fetchPaymentMethodLabel(String paymentIntentId, String chargeId) {
        Stripe.apiKey = secretKey;

        if (chargeId != null && !chargeId.isBlank() && chargeId.startsWith("ch_")) {
            try {
                Charge charge = Charge.retrieve(chargeId);
                String fromCharge = formatPaymentMethodDetails(charge.getPaymentMethodDetails());
                if (fromCharge != null) {
                    return fromCharge;
                }
            } catch (StripeException e) {
                log.warn("Unable to retrieve payment method from charge {}", chargeId, e);
            }
        }

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return null;
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String fromIntent = resolvePaymentMethodFromIntent(paymentIntent);
            if (fromIntent != null) {
                return fromIntent;
            }
        } catch (StripeException e) {
            log.warn("Unable to retrieve payment method from payment intent {}", paymentIntentId, e);
        }

        return null;
    }

    private String resolvePaymentMethodFromIntent(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentMethod() != null) {
            try {
                com.stripe.model.PaymentMethod paymentMethod =
                        com.stripe.model.PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
                String formatted = formatStripePaymentMethod(paymentMethod);
                if (formatted != null) {
                    return formatted;
                }
            } catch (StripeException e) {
                log.warn("Unable to retrieve payment method object {}", paymentIntent.getPaymentMethod(), e);
            }
        }

        if (paymentIntent.getPaymentMethodTypes() != null && !paymentIntent.getPaymentMethodTypes().isEmpty()) {
            String primaryType = paymentIntent.getPaymentMethodTypes().get(0);
            if ("affirm".equals(primaryType)) {
                return "Affirm";
            }
            return capitalize(primaryType.replace('_', ' '));
        }

        return null;
    }

    private String formatPaymentMethodDetails(Charge.PaymentMethodDetails details) {
        if (details == null) {
            return null;
        }
        if ("affirm".equals(details.getType())) {
            return "Affirm";
        }
        if (details.getCard() != null) {
            return formatCardBrand(details.getCard().getBrand()) + " - " + details.getCard().getLast4();
        }
        if (details.getLink() != null) {
            return "Link";
        }
        if (details.getUsBankAccount() != null) {
            return "Bank account - " + details.getUsBankAccount().getLast4();
        }
        String type = details.getType();
        return type != null ? capitalize(type.replace('_', ' ')) : null;
    }

    private String formatStripePaymentMethod(com.stripe.model.PaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.getType() == null) {
            return null;
        }
        return switch (paymentMethod.getType()) {
            case "card" -> {
                com.stripe.model.PaymentMethod.Card card = paymentMethod.getCard();
                if (card != null && card.getLast4() != null) {
                    yield formatCardBrand(card.getBrand()) + " - " + card.getLast4();
                }
                yield "Card";
            }
            case "affirm" -> "Affirm";
            case "link" -> "Link";
            case "us_bank_account" -> {
                com.stripe.model.PaymentMethod.UsBankAccount bank = paymentMethod.getUsBankAccount();
                if (bank != null && bank.getLast4() != null) {
                    yield "Bank account - " + bank.getLast4();
                }
                yield "Bank account";
            }
            default -> capitalize(paymentMethod.getType().replace('_', ' '));
        };
    }

    private String formatCardBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            return "Card";
        }
        return brand.toUpperCase();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private Subscription findOrCreateSubscription(Session session, User user, Plan plan) {
        Optional<Subscription> existing = subscriptionRepository.findByStripeSubscriptionId(session.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return createSubscription(session, user, plan);
        } catch (DataIntegrityViolationException ex) {
            log.info("Subscription already exists for checkout session {}, reusing existing record", session.getId());
            return subscriptionRepository.findByStripeSubscriptionId(session.getId())
                    .orElseThrow(() -> ex);
        }
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
                boolean updated = false;
                if (payment.getSubscription() == null) {
                    payment.setSubscription(subscription);
                    updated = true;
                }
                if (payment.getMethod() == null || payment.getMethod().isBlank()) {
                    String method = fetchPaymentMethodLabel(paymentIntentId, payment.getStripeChargeId());
                    if (method != null) {
                        payment.setMethod(method);
                        updated = true;
                    }
                }
                if (updated) {
                    payment = paymentRepository.save(payment);
                }
                return payment;
            }
        }

        Payment payment = buildPaymentFromSession(session, user, subscription);
        try {
            return paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            if (paymentIntentId != null) {
                log.info("Payment already exists for paymentIntent {}, reusing existing record", paymentIntentId);
                return paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
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
        payment.setMethod(fetchPaymentMethodLabel(paymentIntent.getId(), paymentIntent.getLatestCharge()));
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
                String latestCharge = paymentIntent.getLatestCharge();
                payment.setStripeChargeId(latestCharge);
                payment.setReceiptUrl(fetchReceiptUrl(latestCharge));
                payment.setMethod(resolvePaymentMethodFromIntent(paymentIntent));
                if (payment.getMethod() == null) {
                    payment.setMethod(fetchPaymentMethodLabel(session.getPaymentIntent(), latestCharge));
                }
            } catch (StripeException e) {
                log.warn("Unable to retrieve payment intent {} for session {}",
                        session.getPaymentIntent(), session.getId(), e);
            }
        }

        return payment;
    }

    private String fetchReceiptUrl(String chargeId) {
        if (chargeId == null || chargeId.isBlank() || !chargeId.startsWith("ch_")) {
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
