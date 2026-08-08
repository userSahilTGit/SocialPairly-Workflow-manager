package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminStatsDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionOption;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserAnswer;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldReturnStatsForUsersAndQuestions() {
        User user1 = new User();
        user1.setId(1L);
        user1.setRole(Role.USER);
        user1.setProfileCompleted(true);
        user1.setCreatedAt(LocalDateTime.now().minusDays(1));

        User user2 = new User();
        user2.setId(2L);
        user2.setRole(Role.ADMIN);
        user2.setProfileCompleted(false);
        user2.setCreatedAt(LocalDateTime.now().minusDays(5));

        Question question = new Question();
        question.setId(10L);
        question.setQuestionText("Favorite fruit?");
        question.setType(QuestionType.SINGLE_CHOICE);

        QuestionOption optionA = new QuestionOption();
        optionA.setOptionText("Apple");
        optionA.setQuestion(question);
        QuestionOption optionB = new QuestionOption();
        optionB.setOptionText("Banana");
        optionB.setQuestion(question);
        question.setOptions(List.of(optionA, optionB));

        UserAnswer answer = new UserAnswer();
        answer.setQuestion(question);
        answer.setAnswerValue("Apple");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(questionRepository.count()).thenReturn(1L);
        when(questionRepository.findAll()).thenReturn(List.of(question));
        when(answerRepository.findAll()).thenReturn(List.of(answer));

        AdminStatsDto stats = adminService.getStats();

        assertEquals(2, stats.totalUsers());
        assertEquals(1, stats.totalAdmins());
        assertEquals(1, stats.completedProfiles());
        assertEquals(1, stats.incompleteProfiles());
        assertEquals(1L, stats.totalQuestions());
        assertTrue(stats.answerDistribution().containsKey("Favorite fruit?"));
        assertEquals(2, stats.answerDistribution().get("Favorite fruit?").size());
        assertEquals("Apple", stats.answerDistribution().get("Favorite fruit?").get(0).label());
        assertEquals(1, stats.answerDistribution().get("Favorite fruit?").get(0).count());
    }
}
