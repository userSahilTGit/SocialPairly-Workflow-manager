package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionOption;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceAdditionalTests {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void shouldUpdateQuestionWhenFound() {
        Question existing = new Question();
        existing.setId(1L);
        existing.setQuestionText("Old text");
        existing.setOptions(new java.util.ArrayList<>());
        existing.getOptions().add(new QuestionOption());

        QuestionRequest request = new QuestionRequest(
                "New text",
                QuestionType.SINGLE_CHOICE,
                "general",
                true,
                true,
                List.of("Option 1", "Option 2")
        );

        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        Question updated = questionService.update(1L, request);

        assertEquals("New text", updated.getQuestionText());
        assertEquals(2, updated.getOptions().size());
        assertEquals("Option 1", updated.getOptions().get(0).getOptionText());
    }

    @Test
    void shouldThrowWhenUpdatingMissingQuestion() {
        when(questionRepository.findById(2L)).thenReturn(Optional.empty());

        QuestionRequest request = new QuestionRequest("New", QuestionType.TEXT, "cat", false, false, null);

        assertThrows(ResourceNotFoundException.class, () -> questionService.update(2L, request));
    }

    @Test
    void shouldDeleteQuestionWhenExists() {
        when(questionRepository.existsById(3L)).thenReturn(true);

        questionService.delete(3L);

        verify(questionRepository).deleteById(3L);
    }

    @Test
    void shouldThrowWhenDeletingMissingQuestion() {
        when(questionRepository.existsById(4L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> questionService.delete(4L));
    }
}
