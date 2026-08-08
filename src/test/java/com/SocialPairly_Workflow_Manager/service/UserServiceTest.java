package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private UserAnswerRepository answerRepository;

    @Mock
    private VideoAccessRequestRepository videoAccessRequestRepository;

    @Mock
    private UserMediaRepository userMediaRepository;

    @Mock
    private BankDetailsRepository bankDetailsRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldDeleteAccountForValidUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        DeleteAccountRequest request = new DeleteAccountRequest("secret123");

        when(passwordEncoder.matches(eq("secret123"), eq("encoded-password"))).thenReturn(true);
        when(answerRepository.findByUserId(eq(1L))).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(eq(1L))).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.deleteAccount(user, request));

        verify(videoAccessRequestRepository).deleteByRequesterId(1L);
        verify(videoAccessRequestRepository).deleteByOwnerId(1L);
        verify(userMediaRepository).deleteByUserId(1L);
        verify(bankDetailsRepository).deleteByUserId(1L);
        verify(refundRepository).deleteByUser_Id(1L);
        verify(paymentRepository).deleteByUser_Id(1L);
        verify(subscriptionRepository).deleteByUser_Id(1L);
        verify(answerRepository).deleteAll(any());
        verify(profileRepository, never()).delete(any());
        verify(userRepository).delete(user);
    }

    @Test
    void shouldPreventAdminDeletion() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);

        DeleteAccountRequest request = new DeleteAccountRequest("secret123");

        when(passwordEncoder.matches(eq("secret123"), eq("encoded-password"))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(user, request));
        verify(userRepository, never()).delete(any());
    }
}
