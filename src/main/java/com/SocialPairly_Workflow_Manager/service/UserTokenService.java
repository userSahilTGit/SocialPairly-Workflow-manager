package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.RefundTokenAdjustment;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Plan;
import com.SocialPairly_Workflow_Manager.entity.Subscription;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class UserTokenService {

    private static final Logger log = LoggerFactory.getLogger(UserTokenService.class);
    public static final int DEFAULT_NEW_USER_TOKENS = 50;

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public UserTokenService(UserRepository userRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    public int getBalance(User user) {
        return user != null ? user.getUserTokens() : 0;
    }

    @Transactional
    public int deductTokens(User user, int amount) {
        if (amount <= 0) {
            throw new BadRequestException("Invalid token amount");
        }
        if (user.getUserTokens() < amount) {
            throw new BadRequestException(
                    "Insufficient tokens. You need " + amount + " token" + (amount == 1 ? "" : "s")
                            + " but only have " + user.getUserTokens() + ".");
        }
        user.setUserTokens(user.getUserTokens() - amount);
        userRepository.save(user);
        log.info("Deducted {} tokens from userId={}, new balance={}", amount, user.getId(), user.getUserTokens());
        return user.getUserTokens();
    }

    @Transactional
    public int creditTokensFromPlan(User user, Plan plan) {
        int tokensToAdd = parsePlanTokens(plan != null ? plan.getTokensIncluded() : null);
        if (tokensToAdd <= 0) {
            if (plan != null) {
                log.warn("Plan id={} has no numeric tokens_included value: '{}'",
                        plan.getId(), plan != null ? plan.getTokensIncluded() : null);
            }
            return user.getUserTokens();
        }
        user.setUserTokens(user.getUserTokens() + tokensToAdd);
        userRepository.save(user);
        log.info("Credited {} tokens to userId={} from planId={}, new balance={}",
                tokensToAdd, user.getId(), plan.getId(), user.getUserTokens());
        return user.getUserTokens();
    }

    /**
     * Credits plan tokens for any succeeded payments linked to the subscription that
     * have not been fulfilled yet. Fixes webhook/confirm-session race and backfills
     * purchases made before token crediting was deployed.
     */
    @Transactional
    public void ensureSubscriptionTokensCredited(User user, Subscription subscription) {
        if (user == null || subscription == null || subscription.getPlan() == null) {
            return;
        }
        Plan plan = subscription.getPlan();
        for (Payment payment : paymentRepository.findBySubscription_Id(subscription.getId())) {
            creditTokensForPaymentIfNeeded(user, plan, payment);
        }
        for (Payment payment : paymentRepository.findUncreditedSucceededPaymentsForUser(user.getId())) {
            if (payment.getSubscription() == null
                    || payment.getSubscription().getId().equals(subscription.getId())) {
                creditTokensForPaymentIfNeeded(user, plan, payment);
            }
        }
    }

    @Transactional
    public void creditTokensForPaymentIfNeeded(User user, Plan plan, Payment payment) {
        if (payment == null || payment.isTokensCredited()) {
            return;
        }
        if (!"succeeded".equalsIgnoreCase(payment.getStatus())) {
            return;
        }
        creditTokensFromPlan(user, plan);
        payment.setTokensCredited(true);
        paymentRepository.save(payment);
        log.info("Marked paymentId={} tokens credited for userId={}", payment.getId(), user.getId());
    }

    /**
     * Claws back plan tokens when a refund payout is finalized.
     * <ul>
     *   <li>If {@code user_token >= plan_token}: subtract plan tokens; no refund amount change.</li>
     *   <li>If {@code user_token < plan_token}: charge for excess tokens used
     *       ({@code plan_token - user_token}) at {@code planPaymentAmount / plan_token},
     *       and set {@code user_token = 0}.</li>
     * </ul>
     * Non-numeric / UNLIMITED plans are skipped (no clawback).
     */
    @Transactional
    public RefundTokenAdjustment applyPlanTokenClawbackOnRefund(User user, Plan plan, BigDecimal planPaymentAmount) {
        if (user == null) {
            return RefundTokenAdjustment.none(0);
        }

        int planTokens = parsePlanTokens(plan != null ? plan.getTokensIncluded() : null);
        int previousBalance = user.getUserTokens();
        if (planTokens <= 0) {
            log.info("Skipping plan token clawback for userId={} (no finite plan tokens)", user.getId());
            return RefundTokenAdjustment.none(previousBalance);
        }

        if (previousBalance >= planTokens) {
            int newBalance = previousBalance - planTokens;
            user.setUserTokens(newBalance);
            userRepository.save(user);
            log.info("Refund clawback: removed {} plan tokens from userId={}, balance {} -> {}",
                    planTokens, user.getId(), previousBalance, newBalance);
            return new RefundTokenAdjustment(
                    previousBalance, newBalance, planTokens, planTokens, 0, BigDecimal.ZERO);
        }

        int excessTokensUsed = planTokens - previousBalance;
        BigDecimal paymentAmount = planPaymentAmount != null ? planPaymentAmount : BigDecimal.ZERO;
        BigDecimal tokenUsageCost = BigDecimal.ZERO;
        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costPerToken = paymentAmount.divide(
                    BigDecimal.valueOf(planTokens), 6, RoundingMode.HALF_UP);
            tokenUsageCost = costPerToken
                    .multiply(BigDecimal.valueOf(excessTokensUsed))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        user.setUserTokens(0);
        userRepository.save(user);
        log.info(
                "Refund clawback: userId={} balance {} < planTokens {}; excessUsed={}, tokenCost={}, balance set to 0",
                user.getId(), previousBalance, planTokens, excessTokensUsed, tokenUsageCost);
        return new RefundTokenAdjustment(
                previousBalance, 0, planTokens, previousBalance, excessTokensUsed, tokenUsageCost);
    }

    /**
     * Parses a plan's tokens_included display value into a numeric credit amount.
     * Non-numeric values such as "UNLIMITED" return 0 (no finite credit).
     */
    public static int parsePlanTokens(String tokensIncluded) {
        if (tokensIncluded == null || tokensIncluded.isBlank()) {
            return 0;
        }
        String trimmed = tokensIncluded.trim();
        if (trimmed.equalsIgnoreCase("UNLIMITED")) {
            return 0;
        }
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digitsOnly);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
