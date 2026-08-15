package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.*;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanUpgradeRequestRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
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
import java.util.List;
import java.util.Optional;

@Service
public class PlanUpgradeService {

    private static final Logger log = LoggerFactory.getLogger(PlanUpgradeService.class);
    private static final List<PlanUpgradeStatus> TERMINAL_STATUSES =
            List.of(PlanUpgradeStatus.Completed, PlanUpgradeStatus.Rejected);
    private static final List<RefundStatus> REFUND_TERMINAL_STATUSES =
            List.of(RefundStatus.Completed, RefundStatus.Rejected);

    private final PlanUpgradeRequestRepository upgradeRequestRepository;
    private final RefundRepository refundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.merchant.name:SocialPairly}")
    private String merchantName;

    @Value("${app.support.email:sahil.t@socialpairly.com}")
    private String supportEmail;

    public PlanUpgradeService(PlanUpgradeRequestRepository upgradeRequestRepository,
                              RefundRepository refundRepository,
                              SubscriptionRepository subscriptionRepository,
                              PlanRepository planRepository,
                              PaymentRepository paymentRepository,
                              UserRepository userRepository,
                              CurrentUserService currentUserService) {
        this.upgradeRequestRepository = upgradeRequestRepository;
        this.refundRepository = refundRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Optional<PlanUpgradeDto> getCurrentUserUpgrade() {
        User user = currentUserService.getCurrentUser();
        return upgradeRequestRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId())
                .map(request -> PlanUpgradeDto.from(request, user.getUserTokens()));
    }

    @Transactional(readOnly = true)
    public List<PlanDto> getEligibleUpgradePlans() {
        User user = currentUserService.getCurrentUser();
        int userTokens = user.getUserTokens();
        String currentPlanName = subscriptionRepository
                .findActiveSubscriptionsForUser(user.getId(), LocalDateTime.now())
                .stream()
                .findFirst()
                .map(sub -> sub.getPlan() != null ? sub.getPlan().getPlanName() : null)
                .orElse(null);

        return planRepository.findByIsActiveTrueOrderByDurationDaysAsc().stream()
                .filter(plan -> UserTokenService.parsePlanTokens(plan.getTokensIncluded()) > userTokens)
                .filter(plan -> currentPlanName == null
                        || !currentPlanName.equalsIgnoreCase(plan.getPlanName()))
                .map(PlanDto::from)
                .toList();
    }

    @Transactional
    public PlanUpgradeDto submitUpgradeRequest(PlanUpgradeSubmitRequest request) {
        User user = currentUserService.getCurrentUser();

        if (upgradeRequestRepository.existsByUser_IdAndStatusNotIn(user.getId(), TERMINAL_STATUSES)) {
            throw new BadRequestException("You already have an active plan upgrade request in progress");
        }
        if (refundRepository.existsByUser_IdAndStatusNotIn(user.getId(), REFUND_TERMINAL_STATUSES)) {
            throw new BadRequestException("You cannot request a plan upgrade while a refund request is active");
        }

        Subscription subscription = subscriptionRepository
                .findActiveSubscriptionsForUser(user.getId(), LocalDateTime.now())
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active subscription found to upgrade"));

        Plan upgradePlan = planRepository.findById(request.upgradePlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade plan not found"));
        if (!upgradePlan.isActive()) {
            throw new BadRequestException("Selected upgrade plan is not available");
        }

        int planTokens = UserTokenService.parsePlanTokens(upgradePlan.getTokensIncluded());
        if (planTokens <= user.getUserTokens()) {
            throw new BadRequestException("Upgrade plan must include more tokens than your current balance");
        }

        String currentPlanName = subscription.getPlan() != null
                ? subscription.getPlan().getPlanName()
                : "Unknown";

        if (upgradePlan.getPlanName() != null
                && upgradePlan.getPlanName().equalsIgnoreCase(currentPlanName)) {
            throw new BadRequestException("Please select a different plan than your current membership");
        }

        PlanUpgradeRequest upgradeRequest = new PlanUpgradeRequest();
        upgradeRequest.setUser(user);
        upgradeRequest.setCurrentPlan(currentPlanName);
        upgradeRequest.setUpgradePlan(upgradePlan.getPlanName());
        upgradeRequest.setReason(request.reason().trim());
        upgradeRequest.setStatus(PlanUpgradeStatus.Started);
        upgradeRequest.setAction(PlanUpgradeAction.Requested);
        upgradeRequest.setExtraToken(0);
        upgradeRequest.setExtraAmount(BigDecimal.ZERO);

        upgradeRequest = upgradeRequestRepository.save(upgradeRequest);
        log.info("Plan upgrade request created id={} userId={} upgradePlan={}",
                upgradeRequest.getId(), user.getId(), upgradePlan.getPlanName());

        return PlanUpgradeDto.from(upgradeRequest, user.getUserTokens());
    }

    @Transactional(readOnly = true)
    public List<AdminPlanUpgradeListDto> listUpgradesForAdmin() {
        return upgradeRequestRepository.findAllWithDetails().stream()
                .map(AdminPlanUpgradeListDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPlanUpgradeDetailDto getUpgradeDetailForAdmin(Long id) {
        return AdminPlanUpgradeDetailDto.from(getAdminRequestOrThrow(id));
    }

    @Transactional
    public AdminPlanUpgradeDetailDto adminApprove(Long id) {
        PlanUpgradeRequest request = getAdminRequestOrThrow(id);
        validateAdminActionAllowed(request);

        User user = request.getUser();
        Plan upgradePlan = resolveUpgradePlan(request.getUpgradePlan());
        UpgradeCost cost = calculateUpgradeCost(
                user.getUserTokens(),
                UserTokenService.parsePlanTokens(upgradePlan.getTokensIncluded()),
                upgradePlan.getAmount()
        );

        request.setExtraToken(cost.extraToken());
        request.setExtraAmount(cost.extraAmount());
        request.setStatus(PlanUpgradeStatus.InProgress);
        request.setAction(PlanUpgradeAction.Approved);
        upgradeRequestRepository.save(request);

        log.info("Admin approved upgrade id={} extraToken={} extraAmount={}",
                id, cost.extraToken(), cost.extraAmount());
        return AdminPlanUpgradeDetailDto.from(request);
    }

    @Transactional
    public AdminPlanUpgradeDetailDto adminReject(Long id) {
        PlanUpgradeRequest request = getAdminRequestOrThrow(id);
        validateAdminActionAllowed(request);

        request.setStatus(PlanUpgradeStatus.Rejected);
        request.setAction(PlanUpgradeAction.Closed);
        upgradeRequestRepository.save(request);

        log.info("Admin rejected upgrade id={}", id);
        return AdminPlanUpgradeDetailDto.from(request);
    }

    /**
     * Fulfills an approved upgrade after Stripe checkout succeeds.
     * Cancels the old subscription, starts the upgrade plan, credits extra tokens,
     * and marks the upgrade request completed.
     */
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public CheckoutConfirmDto fulfillUpgradeCheckout(Session session, Long expectedUserId) {
        if (!"paid".equals(session.getPaymentStatus())) {
            throw new BadRequestException("Checkout session is not paid. Status: " + session.getPaymentStatus());
        }

        Long userId = parseLong(session.getMetadata().get("userId"), "userId");
        Long planId = parseLong(session.getMetadata().get("planId"), "planId");
        Long upgradeRequestId = parseLong(session.getMetadata().get("upgradeRequestId"), "upgradeRequestId");

        if (expectedUserId != null && !expectedUserId.equals(userId)) {
            throw new BadRequestException("Checkout session does not belong to the current user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        PlanUpgradeRequest upgradeRequest = upgradeRequestRepository.findByIdWithDetails(upgradeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found: " + upgradeRequestId));

        if (!upgradeRequest.getUser().getId().equals(userId)) {
            throw new BadRequestException("Upgrade request does not belong to this user");
        }
        if (upgradeRequest.getStatus() == PlanUpgradeStatus.Completed
                && upgradeRequest.getPayment() != null) {
            Payment existingPayment = upgradeRequest.getPayment();
            return new CheckoutConfirmDto(
                    findActiveSubscriptionDto(userId).orElse(null),
                    buildReceiptDto(existingPayment, plan, user),
                    existingPayment.getStatus()
            );
        }
        if (upgradeRequest.getStatus() != PlanUpgradeStatus.InProgress
                || upgradeRequest.getAction() != PlanUpgradeAction.Approved) {
            throw new BadRequestException("Upgrade request is not approved for payment");
        }

        cancelActiveSubscriptions(userId, session.getId());

        Subscription subscription = findOrCreateUpgradeSubscription(session, user, plan);
        Payment payment = findOrCreatePayment(session, user, subscription);

        int extraTokens = upgradeRequest.getExtraToken() != null ? upgradeRequest.getExtraToken() : 0;
        if (!payment.isTokensCredited() && extraTokens > 0) {
            user.setUserTokens(user.getUserTokens() + extraTokens);
            userRepository.save(user);
            payment.setTokensCredited(true);
            paymentRepository.save(payment);
            log.info("Credited {} upgrade tokens to userId={}, new balance={}",
                    extraTokens, userId, user.getUserTokens());
        } else if (!payment.isTokensCredited()) {
            payment.setTokensCredited(true);
            paymentRepository.save(payment);
        }

        upgradeRequest.setPayment(payment);
        upgradeRequest.setStatus(PlanUpgradeStatus.Completed);
        upgradeRequest.setAction(PlanUpgradeAction.Closed);
        upgradeRequestRepository.save(upgradeRequest);

        log.info("Upgrade fulfilled requestId={} userId={} newPlanId={} paymentId={}",
                upgradeRequestId, userId, planId, payment.getId());

        return new CheckoutConfirmDto(
                SubscriptionDto.from(subscription),
                buildReceiptDto(payment, plan, user),
                payment.getStatus()
        );
    }

    public static UpgradeCost calculateUpgradeCost(int userTokens, int planTokens, BigDecimal planPrice) {
        if (planTokens <= 0) {
            throw new BadRequestException("Upgrade plan does not have a valid token amount");
        }
        BigDecimal price = planPrice != null ? planPrice : BigDecimal.ZERO;

        if (userTokens < UserTokenService.DEFAULT_NEW_USER_TOKENS) {
            return new UpgradeCost(planTokens, price.setScale(2, RoundingMode.HALF_UP));
        }

        int remainingTokens = userTokens - UserTokenService.DEFAULT_NEW_USER_TOKENS;
        int requiredTokens = planTokens - remainingTokens;
        if (requiredTokens < 0) {
            requiredTokens = 0;
        }

        BigDecimal extraAmount = BigDecimal.ZERO;
        if (requiredTokens > 0 && price.compareTo(BigDecimal.ZERO) > 0) {
            extraAmount = BigDecimal.valueOf(requiredTokens)
                    .divide(BigDecimal.valueOf(planTokens), 6, RoundingMode.HALF_UP)
                    .multiply(price)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new UpgradeCost(requiredTokens, extraAmount);
    }

    public record UpgradeCost(int extraToken, BigDecimal extraAmount) {
    }

    private Plan resolveUpgradePlan(String planName) {
        return planRepository.findFirstByPlanNameIgnoreCase(planName)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade plan not found: " + planName));
    }

    private PlanUpgradeRequest getAdminRequestOrThrow(Long id) {
        return upgradeRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade request not found: " + id));
    }

    private void validateAdminActionAllowed(PlanUpgradeRequest request) {
        if (request.getStatus() == PlanUpgradeStatus.Completed) {
            throw new BadRequestException("This upgrade request has already been completed");
        }
        if (request.getStatus() == PlanUpgradeStatus.Rejected
                || request.getAction() == PlanUpgradeAction.Closed) {
            throw new BadRequestException("This upgrade request has been closed");
        }
        if (request.getAction() == PlanUpgradeAction.Approved) {
            throw new BadRequestException("This upgrade request has already been approved");
        }
    }

    private void cancelActiveSubscriptions(Long userId, String checkoutSessionId) {
        List<Subscription> activeSubscriptions =
                subscriptionRepository.findActiveSubscriptionsForUser(userId, LocalDateTime.now());
        for (Subscription subscription : activeSubscriptions) {
            if (checkoutSessionId != null
                    && checkoutSessionId.equals(subscription.getStripeSubscriptionId())) {
                continue;
            }
            subscription.setStatus("cancelled");
            subscription.setCanceledAt(LocalDateTime.now());
            subscription.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(subscription);
            log.info("Cancelled subscription id={} for upgrade userId={}", subscription.getId(), userId);
        }
    }

    private Subscription findOrCreateUpgradeSubscription(Session session, User user, Plan plan) {
        Optional<Subscription> existing = subscriptionRepository.findByStripeSubscriptionId(session.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

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

        try {
            return subscriptionRepository.save(subscription);
        } catch (DataIntegrityViolationException ex) {
            return subscriptionRepository.findByStripeSubscriptionId(session.getId())
                    .orElseThrow(() -> ex);
        }
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

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setSubscription(subscription);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setAmount(centsToDollars(session.getAmountTotal()));
        payment.setCurrency(session.getCurrency() != null ? session.getCurrency() : "usd");
        payment.setStatus("succeeded");

        if (paymentIntentId != null) {
            Stripe.apiKey = secretKey;
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                String latestCharge = paymentIntent.getLatestCharge();
                payment.setStripeChargeId(latestCharge);
                payment.setReceiptUrl(fetchReceiptUrl(latestCharge));
                payment.setMethod(fetchPaymentMethodLabel(paymentIntent));
            } catch (StripeException e) {
                log.warn("Unable to enrich upgrade payment from intent {}", paymentIntentId, e);
            }
        }

        try {
            return paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            if (paymentIntentId != null) {
                return paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private Optional<SubscriptionDto> findActiveSubscriptionDto(Long userId) {
        return subscriptionRepository.findActiveSubscriptionsForUser(userId, LocalDateTime.now())
                .stream()
                .findFirst()
                .map(SubscriptionDto::from);
    }

    private ReceiptDto buildReceiptDto(Payment payment, Plan plan, User user) {
        return new ReceiptDto(
                null,
                plan.getPlanName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                user.getEmail(),
                payment.getReceiptUrl(),
                merchantName,
                supportEmail,
                payment.getMethod()
        );
    }

    private String fetchReceiptUrl(String chargeId) {
        if (chargeId == null || chargeId.isBlank() || !chargeId.startsWith("ch_")) {
            return null;
        }
        Stripe.apiKey = secretKey;
        try {
            return Charge.retrieve(chargeId).getReceiptUrl();
        } catch (StripeException e) {
            return null;
        }
    }

    private String fetchPaymentMethodLabel(PaymentIntent paymentIntent) {
        if (paymentIntent == null) return null;
        try {
            if (paymentIntent.getPaymentMethod() != null) {
                com.stripe.model.PaymentMethod pm =
                        com.stripe.model.PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
                if (pm != null && "card".equals(pm.getType()) && pm.getCard() != null) {
                    String brand = pm.getCard().getBrand() != null
                            ? pm.getCard().getBrand().toUpperCase() : "Card";
                    return brand + " - " + pm.getCard().getLast4();
                }
            }
        } catch (StripeException e) {
            log.warn("Unable to resolve payment method for upgrade", e);
        }
        return null;
    }

    private BigDecimal centsToDollars(Long amountInCents) {
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
