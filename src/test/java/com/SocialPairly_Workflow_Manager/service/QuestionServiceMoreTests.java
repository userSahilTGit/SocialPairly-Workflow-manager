package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceMoreTests {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void shouldCreateTextQuestionWithoutOptions() {
        QuestionRequest request = new QuestionRequest("Text question", QuestionType.TEXT, "general", true, true, null);
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        Question question = questionService.create(request);

        assertNotNull(question);
        assertEquals(QuestionType.TEXT, question.getType());
        assertTrue(question.getOptions().isEmpty());
    }

    @Test
    void shouldReturnActiveQuestionsEvenWhenAnswerNotPresent() {
        var user = new com.SocialPairly_Workflow_Manager.entity.User();
        user.setId(1L);
        Question question = new Question();
        question.setId(20L);
        question.setQuestionText("Q");
        question.setActive(true);

        when(questionRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(question));
        when(answerRepository.findByUserId(1L)).thenReturn(List.of());

        var result = questionService.getActiveQuestionsForUser(user);

        assertEquals(1, result.size());
        assertNull(result.get(0).answerValue());
    }
}
