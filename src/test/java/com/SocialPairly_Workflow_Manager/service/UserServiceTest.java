package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.ChangePasswordRequest;
import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.EventParticipantRepository;
import com.SocialPairly_Workflow_Manager.repository.PlanUpgradeRequestRepository;
import com.SocialPairly_Workflow_Manager.repository.UserPersonalityProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private PlanUpgradeRequestRepository planUpgradeRequestRepository;

    @Mock
    private UserPersonalityProfileRepository personalityProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldSendAccountDeleteOtp() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(otpService.generateAndStore(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "test@example.com", 5))
                .thenReturn("1234");

        Map<String, String> result = userService.sendAccountDeleteOtp(user);

        assertEquals("5", result.get("expiresInMinutes"));
        verify(emailService).sendAccountDeleteOtpEmail(user, "1234");
    }

    @Test
    void shouldMapMailAuthFailureToBadRequestWhenSendingDeleteOtp() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(otpService.generateAndStore(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "test@example.com", 5))
                .thenReturn("1234");
        doThrow(new org.springframework.mail.MailAuthenticationException("Authentication failed"))
                .when(emailService).sendAccountDeleteOtpEmail(user, "1234");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.sendAccountDeleteOtp(user));
        assertEquals("Failed to send deletion code. Please try again later.", ex.getMessage());
    }

    @Test
    void shouldMapMessagingExceptionToBadRequestWhenSendingDeleteOtp() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(otpService.generateAndStore(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "test@example.com", 5))
                .thenReturn("9999");
        doThrow(new jakarta.mail.MessagingException("smtp down"))
                .when(emailService).sendAccountDeleteOtpEmail(user, "9999");

        assertThrows(BadRequestException.class, () -> userService.sendAccountDeleteOtp(user));
    }

    @Test
    void shouldRejectSendDeleteOtpForAdmin() {
        User admin = new User();
        admin.setId(9L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        assertThrows(BadRequestException.class, () -> userService.sendAccountDeleteOtp(admin));
        verify(otpService, never()).generateAndStore(any(), any(), anyInt());
    }

    @Test
    void shouldRejectSendDeleteOtpWhenEmailMissing() {
        User user = new User();
        user.setId(1L);
        user.setEmail("  ");
        user.setRole(Role.USER);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.sendAccountDeleteOtp(user));
        assertEquals("No email on your account to send a deletion code", ex.getMessage());
    }

    @Test
    void shouldRejectDeleteWhenEmailMissing() {
        User user = new User();
        user.setId(1L);
        user.setEmail(null);
        user.setRole(Role.USER);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.deleteAccount(user, new DeleteAccountRequest("1234")));
        assertEquals("No email on your account to verify deletion", ex.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldRejectPasswordChangeWhenAccountHasNoPassword() {
        User user = new User();
        user.setId(4L);
        user.setPassword("  ");
        ChangePasswordRequest request = new ChangePasswordRequest("old", "new-pass", "new-pass");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.changePassword(user, request));
        assertEquals("This account does not have a password to change.", ex.getMessage());
    }

    @Test
    void shouldRejectPasswordChangeWhenNewMatchesCurrent() {
        User user = new User();
        user.setId(5L);
        user.setPassword("encoded");
        ChangePasswordRequest request = new ChangePasswordRequest("same", "same", "same");

        when(passwordEncoder.matches("same", "encoded")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.changePassword(user, request));
        assertEquals("New password must be different from your current password", ex.getMessage());
    }

    @Test
    void findOptionalByIdentifierShouldReturnEmptyForBlank() {
        assertTrue(userService.findOptionalByIdentifier("  ").isEmpty());
        assertTrue(userService.findOptionalByIdentifier(null).isEmpty());
    }

    @Test
    void shouldDeleteAccountForValidUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        DeleteAccountRequest request = new DeleteAccountRequest("1234");

        when(answerRepository.findByUserId(eq(1L))).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(eq(1L))).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.deleteAccount(user, request));

        verify(otpService).verify(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "test@example.com", "1234");
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
        verify(otpService).clear(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "test@example.com");
    }

    @Test
    void shouldDeleteEventParticipationPersonalityAndUpgradeRequestsBeforeUser() {
        User user = new User();
        user.setId(7L);
        user.setEmail("member@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);

        when(answerRepository.findByUserId(eq(7L))).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(eq(7L))).thenReturn(Optional.empty());

        userService.deleteAccount(user, new DeleteAccountRequest("5678"));

        InOrder order = inOrder(otpService, eventParticipantRepository, personalityProfileRepository,
                planUpgradeRequestRepository, paymentRepository, userRepository);
        order.verify(otpService).verify(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, "member@example.com", "5678");
        order.verify(eventParticipantRepository).deleteByUser_Id(7L);
        order.verify(personalityProfileRepository).deleteByUser_Id(7L);
        order.verify(planUpgradeRequestRepository).deleteByUser_Id(7L);
        order.verify(paymentRepository).deleteByUser_Id(7L);
        order.verify(userRepository).delete(user);
    }

    @Test
    void shouldPreventAdminDeletion() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPhoneNumber("+12025550123");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);

        DeleteAccountRequest request = new DeleteAccountRequest("1234");

        assertThrows(BadRequestException.class, () -> userService.deleteAccount(user, request));
        verify(otpService, never()).verify(any(), any(), any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordMatches() {
        User user = new User();
        user.setId(3L);
        user.setPassword("encoded-old");
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "new-pass", "new-pass");

        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        Map<String, String> result = userService.changePassword(user, request);

        assertEquals("Password updated successfully", result.get("message"));
        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectPasswordChangeWhenConfirmationDiffers() {
        User user = new User();
        user.setPassword("encoded-old");
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "new-pass", "other-pass");

        assertThrows(BadRequestException.class, () -> userService.changePassword(user, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsWrong() {
        User user = new User();
        user.setPassword("encoded-old");
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "new-pass", "new-pass");
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changePassword(user, request));
        verify(userRepository, never()).save(any());
    }
}
