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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAdditionalMoreTests {

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
    void shouldRejectAdminDeleteAccount() {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        admin.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("pass");

        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(admin, request));
    }

    @Test
    void shouldRejectDeleteWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.USER);
        user.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("wrongpass");

        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(user, request));
        verify(userRepository, never()).delete(any());
    }
}
