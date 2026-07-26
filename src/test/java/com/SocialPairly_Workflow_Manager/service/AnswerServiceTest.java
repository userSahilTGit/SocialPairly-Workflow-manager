package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AnswerRequest;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private UserAnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AnswerService answerService;

    @Test
    void shouldSaveNewAnswerWhenMissing() {
        User user = new User();
        user.setId(42L);

        Question question = new Question();
        question.setId(100L);

        AnswerRequest request = new AnswerRequest(100L, "Spring Boot");

        when(questionRepository.findById(eq(100L))).thenReturn(Optional.of(question));
        when(answerRepository.findByUserIdAndQuestionId(eq(42L), eq(100L))).thenReturn(Optional.empty());
        when(answerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        answerService.saveAnswers(user, List.of(request));

        verify(answerRepository).save(any());
    }
}
