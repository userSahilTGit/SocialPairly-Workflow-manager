package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.PaymentRequestDTO;
import com.SocialPairly_Workflow_Manager.dto.PaymentResponseDTO;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
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

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public PaymentService(CurrentUserService currentUserService,
                          PlanRepository planRepository,
                          SubscriptionService subscriptionService) {
        this.currentUserService = currentUserService;
        this.planRepository = planRepository;
        this.subscriptionService = subscriptionService;
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

        SessionCreateParams.PaymentIntentData paymentIntentData =
                SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("userId", user.getId().toString())
                        .putMetadata("planId", plan.getId().toString())
                        .build();

        String successUrl = frontendUrl + "/subscriptions?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendUrl + "/subscriptions?payment=cancelled";

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setCustomerEmail(user.getEmail())
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("userId", user.getId().toString())
                        .putMetadata("planId", plan.getId().toString())
                        .setPaymentIntentData(paymentIntentData)
                        .addLineItem(lineItem)
                        .build();

        log.info("Creating Stripe checkout session for userId={} planId={} product={} amount={} currency={}",
                user.getId(), plan.getId(), productName, amountInCents, currency);

        Session session;
        try {
            session = Session.create(params);
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
