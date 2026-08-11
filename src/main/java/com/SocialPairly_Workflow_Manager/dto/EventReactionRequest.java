package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

public record EventReactionRequest(
        @NotNull Long toUserId,
        @NotNull ReactionType reactionType
) {
}
