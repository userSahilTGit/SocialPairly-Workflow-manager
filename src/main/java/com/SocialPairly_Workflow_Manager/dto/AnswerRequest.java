package com.SocialPairly_Workflow_Manager.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
    @NotNull Long questionId,
    String answerValue
) {}