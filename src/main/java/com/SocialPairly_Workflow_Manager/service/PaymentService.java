package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeAction;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeRequest;
import com.SocialPairly_Workflow_Manager.entity.PlanUpgradeStatus;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanUpgradeRequestRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final CurrentUserService currentUserService;
    private final PlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final PlanUpgradeRequestRepository planUpgradeRequestRepository;

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public PaymentService(CurrentUserService currentUserService,
                          PlanRepository planRepository,
                          SubscriptionService subscriptionService,
                          PlanUpgradeRequestRepository planUpgradeRequestRepository) {
        this.currentUserService = currentUserService;
        this.planRepository = planRepository;
        this.subscriptionService = subscriptionService;
        this.planUpgradeRequestRepository = planUpgradeRequestRepository;
    }

    public PaymentResponseDTO checkoutProducts(PaymentRequestDTO productRequest) {
        Stripe.apiKey = secretKey;

        User user = currentUserService.getCurrentUser();
        subscriptionService.ensureNoActiveSubscription(user);

        Plan plan = resolvePlan(productRequest);

        long amountInCents = resolveAmountInCents(productRequest, plan);
        String currency = productRequest.getCurrency() != null ? productRequest.getCurrency() : "USD";
        long quantity = productRequest.getQuantity() != null ? productRequest.getQuantity() : 1L;
        String productName = productRequest.getName() != null ? productRequest.getName() : plan.getPlanName();

        return createCheckoutSession(user, plan, amountInCents, currency, quantity, productName, null);
    }

    public PaymentResponseDTO checkoutUpgrade(Long upgradeRequestId) {
        Stripe.apiKey = secretKey;

        User user = currentUserService.getCurrentUser();
        PlanUpgradeRequest upgradeRequest = planUpgradeRequestRepository.findByIdWithDetails(upgradeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found: " + upgradeRequestId));

        if (!upgradeRequest.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have access to this upgrade request");
        }
        if (upgradeRequest.getStatus() != PlanUpgradeStatus.InProgress
                || upgradeRequest.getAction() != PlanUpgradeAction.Approved) {
            throw new BadRequestException("Upgrade request must be approved before checkout");
        }
        if (upgradeRequest.getExtraAmount() == null
                || upgradeRequest.getExtraAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No payable amount found for this upgrade request");
        }

        Plan plan = planRepository.findFirstByPlanNameIgnoreCase(upgradeRequest.getUpgradePlan())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Upgrade plan not found: " + upgradeRequest.getUpgradePlan()));

        long amountInCents = upgradeRequest.getExtraAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        String productName = "Plan Upgrade: " + plan.getPlanName();

        return createCheckoutSession(user, plan, amountInCents, "USD", 1L, productName, upgradeRequest.getId());
    }

    private PaymentResponseDTO createCheckoutSession(User user,
                                                     Plan plan,
                                                     long amountInCents,
                                                     String currency,
                                                     long quantity,
                                                     String productName,
                                                     Long upgradeRequestId) {
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(productName)
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(currency)
                        .setUnitAmount(amountInCents)
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(quantity)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams.PaymentIntentData.Builder paymentIntentBuilder =
                SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("userId", user.getId().toString())
                        .putMetadata("planId", plan.getId().toString());

        SessionCreateParams.Builder paramsBuilder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setCustomerEmail(user.getEmail())
                        .setSuccessUrl(frontendUrl + "/subscriptions?payment=success&session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(frontendUrl + "/subscriptions?payment=cancelled")
                        .putMetadata("userId", user.getId().toString())
                        .putMetadata("planId", plan.getId().toString())
                        .addLineItem(lineItem);

        if (upgradeRequestId != null) {
            paymentIntentBuilder
                    .putMetadata("checkoutType", "upgrade")
                    .putMetadata("upgradeRequestId", upgradeRequestId.toString());
            paramsBuilder
                    .putMetadata("checkoutType", "upgrade")
                    .putMetadata("upgradeRequestId", upgradeRequestId.toString());
        }

        paramsBuilder.setPaymentIntentData(paymentIntentBuilder.build());

        log.info("Creating Stripe checkout session for userId={} planId={} product={} amount={} currency={} upgradeRequestId={}",
                user.getId(), plan.getId(), productName, amountInCents, currency, upgradeRequestId);

        Session session;
        try {
            session = Session.create(paramsBuilder.build());
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed", e);
            throw new IllegalStateException("Unable to create payment session", e);
        }

        return PaymentResponseDTO.builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .build();
    }

    private Plan resolvePlan(PaymentRequestDTO productRequest) {
        if (productRequest.getPlanId() == null) {
            throw new BadRequestException("planId is required for checkout");
        }
        return planRepository.findById(productRequest.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
    }

    private long resolveAmountInCents(PaymentRequestDTO productRequest, Plan plan) {
        if (productRequest.getAmount() != null) {
            return productRequest.getAmount();
        }
        return plan.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
