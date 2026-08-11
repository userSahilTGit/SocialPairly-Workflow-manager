package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.ReactionType;

public record EventReactionResponseDto(
        ReactionType reactionType,
        int tokensSpent,
        int tokenBalance
) {
}
