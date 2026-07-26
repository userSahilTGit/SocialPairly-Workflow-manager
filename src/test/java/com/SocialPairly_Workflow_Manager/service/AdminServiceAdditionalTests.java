package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceAdditionalTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldListUsersAsDtos() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPhoneNumber("123");

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> users = adminService.listUsers();

        assertEquals(1, users.size());
        assertEquals("jane@example.com", users.get(0).email());
    }

    @Test
    void shouldReturnEmptyAnswerDistributionWhenNoChoiceQuestionsExist() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(questionRepository.count()).thenReturn(0L);
        Question question = new Question();
        question.setId(10L);
        question.setType(QuestionType.TEXT);
        when(questionRepository.findAll()).thenReturn(List.of(question));

        var stats = adminService.getStats();

        assertEquals(0, stats.totalUsers());
        assertEquals(0, stats.totalQuestions());
        assertTrue(stats.answerDistribution().isEmpty());
        assertEquals(0, stats.newUsersLastDays());
    }
}
