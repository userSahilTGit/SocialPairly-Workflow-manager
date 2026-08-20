package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.DeleteAccountRequest;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.BankDetailsRepository;
import com.SocialPairly_Workflow_Manager.repository.PaymentRepository;
import com.SocialPairly_Workflow_Manager.repository.RefundRepository;
import com.SocialPairly_Workflow_Manager.repository.SubscriptionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserAnswerRepository;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserProfileRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAdditionalTests {

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "defaultCountryCode", "+91");
    }

    @Test
    void findByIdentifierShouldThrowWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findByIdentifier("missing@example.com"));
    }

    @Test
    void findByIdentifierShouldReturnUserByEmail() {
        User user = new User();
        user.setId(5L);
        user.setEmail("ada@example.com");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        assertSame(user, userService.findByIdentifier("  Ada@Example.com "));
    }

    @Test
    void findByIdentifierShouldFallBackToPhoneLookup() {
        User user = new User();
        user.setId(6L);
        user.setPhoneNumber("+91-9876543210");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("+91-9876543210".equals(key) || "+919876543210".equals(key)) {
                return Optional.of(user);
            }
            return Optional.empty();
        });

        assertSame(user, userService.findByIdentifier("9876543210"));
    }

    @Test
    void findOptionalByPhoneIdentifierShouldReturnEmptyWhenNoMatch() {
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        assertTrue(userService.findOptionalByPhoneIdentifier("9876543210").isEmpty());
    }

    @Test
    void updatePhoneShouldNormalizeStoreAndClearVerified() {
        User user = new User();
        user.setId(7L);
        user.setPhoneVerified(true);
        when(userRepository.existsByPhoneNumberAndIdNot(anyString(), eq(7L))).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        Map<String, Object> result = userService.updatePhone(user, "9876543210");

        assertEquals("+91-9876543210", user.getPhoneNumber());
        assertFalse(user.isPhoneVerified());
        assertEquals("Phone number saved. Complete Firebase SMS verification to confirm it.", result.get("message"));
        verify(userRepository).save(user);
    }

    @Test
    void updatePhoneShouldRejectInvalidPhone() {
        User user = new User();
        user.setId(7L);

        assertThrows(BadRequestException.class, () -> userService.updatePhone(user, "123"));
    }

    @Test
    void updatePhoneShouldRejectPhoneOwnedByOtherUser() {
        User user = new User();
        user.setId(7L);
        when(userRepository.existsByPhoneNumberAndIdNot(anyString(), eq(7L))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.updatePhone(user, "9876543210"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccountShouldDeleteProfileAndAnswers() {
        User currentUser = new User();
        currentUser.setId(2L);
        currentUser.setRole(Role.USER);
        currentUser.setPassword("encoded");

        DeleteAccountRequest request = new DeleteAccountRequest("pass");

        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(answerRepository.findByUserId(2L)).thenReturn(java.util.List.of());
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(new UserProfile()));

        userService.deleteAccount(currentUser, request);

        verify(answerRepository).deleteAll(anyList());
        verify(profileRepository).delete(any(UserProfile.class));
        verify(userRepository).delete(currentUser);
    }
}
