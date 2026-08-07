package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.AdminPaymentDto;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Payment;
import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.QuestionType;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

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
        when(subscriptionRepository.findActiveSubscribedUserIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(1L));

        List<UserDto> users = adminService.listUsers();

        assertEquals(1, users.size());
        assertEquals("jane@example.com", users.get(0).email());
        assertEquals("Subscribed", users.get(0).subscriptionDetails());
    }

    @Test
    void shouldListPaymentsForAdmin() {
        User user = new User();
        user.setId(2L);
        user.setFirstName("Pay");
        user.setLastName("User");
        user.setEmail("pay@example.com");

        Payment payment = new Payment();
        payment.setId(50L);
        payment.setUser(user);
        payment.setStripeChargeId("ch_test");
        payment.setAmount(new BigDecimal("19.99"));
        payment.setCurrency("usd");
        payment.setStatus("succeeded");
        payment.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.findAllWithUserOrderByCreatedAtDesc()).thenReturn(List.of(payment));

        List<AdminPaymentDto> payments = adminService.listPayments();

        assertEquals(1, payments.size());
        assertEquals(50L, payments.get(0).id());
        assertEquals("pay@example.com", payments.get(0).userEmail());
        assertEquals("succeeded", payments.get(0).status());
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
