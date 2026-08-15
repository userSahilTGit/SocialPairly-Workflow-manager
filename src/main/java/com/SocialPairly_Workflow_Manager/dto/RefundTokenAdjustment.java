package com.SocialPairly_Workflow_Manager.dto;

import java.math.BigDecimal;

/**
 * Result of clawing back plan tokens when a refund payout is finalized.
 */
public record RefundTokenAdjustment(
        int previousBalance,
        int newBalance,
        int planTokens,
        int tokensRemoved,
        int excessTokensUsed,
        BigDecimal tokenUsageCost
) {
    public static RefundTokenAdjustment none(int balance) {
        return new RefundTokenAdjustment(balance, balance, 0, 0, 0, BigDecimal.ZERO);
    }

    public boolean hasTokenUsageDeduction() {
        return excessTokensUsed > 0
                && tokenUsageCost != null
                && tokenUsageCost.compareTo(BigDecimal.ZERO) > 0;
    }
}
