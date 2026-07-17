package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.QuestionDto;
import com.SocialPairly_Workflow_Manager.dto.QuestionRequest;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void shouldCreateSingleChoiceQuestionWithOptions() {
        QuestionRequest request = new QuestionRequest(
                "What is your favorite color?",
                QuestionType.SINGLE_CHOICE,
                "preferences",
                true,
                true,
                List.of("Red", "Blue", "Green")
        );

        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));

        Question question = questionService.create(request);

        assertNotNull(question);
        assertEquals("What is your favorite color?", question.getQuestionText());
        assertEquals(3, question.getOptions().size());
        assertEquals("Red", question.getOptions().get(0).getOptionText());
    }

    @Test
    void shouldReturnActiveQuestionsWithAnswerValues() {
        User user = new User();
        user.setId(5L);

        Question question = new Question();
        question.setId(100L);
        question.setQuestionText("Favorite programming language?");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setActive(true);

        when(questionRepository.findByActiveTrueOrderByCreatedAtAsc()).thenReturn(List.of(question));

        UserAnswer answer = new UserAnswer();
        answer.setQuestion(question);
        answer.setAnswerValue("Java");

        when(answerRepository.findByUserId(eq(5L))).thenReturn(List.of(answer));

        List<QuestionDto> dtos = questionService.getActiveQuestionsForUser(user);

        assertEquals(1, dtos.size());
        assertEquals("Java", dtos.get(0).answerValue());
        assertEquals("Favorite programming language?", dtos.get(0).questionText());
    }
}
