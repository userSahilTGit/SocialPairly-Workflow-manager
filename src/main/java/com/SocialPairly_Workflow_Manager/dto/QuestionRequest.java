package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record QuestionRequest(
    @NotBlank String questionText,
    QuestionType type,
    String category,
    boolean required,
    boolean active,
    List<String> options
) {}