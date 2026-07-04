package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import java.util.List;

public record QuestionDto(
    Long id,
    String questionText,
    QuestionType type,
    String category,
    boolean required,
    boolean active,
    List<String> options,
    String answerValue
) {
    public static QuestionDto from(Question q, String answerValue) {
        return new QuestionDto(
            q.getId(),
            q.getQuestionText(),
            q.getType(),
            q.getCategory(),
            q.isRequired(),
            q.isActive(),
            q.getOptions().stream().map(o -> o.getOptionText()).toList(),
            answerValue
        );
    }
}