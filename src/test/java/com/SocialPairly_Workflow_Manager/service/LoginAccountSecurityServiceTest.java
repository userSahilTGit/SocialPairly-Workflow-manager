package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.LoginSecurityEvent;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.exception.AccountSecurityException;
import com.SocialPairly_Workflow_Manager.repository.LoginSecurityEventRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAccountSecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginSecurityEventRepository loginSecurityEventRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EmailService emailService;

    private LoginAccountSecurityService service;

    @BeforeEach
    void setUp() {
        service = new LoginAccountSecurityService(
                userRepository, loginSecurityEventRepository, subscriptionService, emailService);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User baseUser(boolean emailVerified) {
        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setEmailVerified(emailVerified);
        user.setFailedLoginAttempts(0);
        return user;
    }

    @Test
    void subscribedUserLocksAfterThreeFailuresWithVisibleMessage() {
        User user = baseUser(true);
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(true);
        when(loginSecurityEventRepository.countByUserIdAndEventTypeSince(eq(10L), eq(LoginSecurityEvent.ACCOUNT_LOCKED), any()))
                .thenReturn(0L);
        when(loginSecurityEventRepository.countByUserIdAndEventType(10L, LoginSecurityEvent.ACCOUNT_LOCKED))
                .thenReturn(0L);

        AccountSecurityException ex = assertThrows(AccountSecurityException.class,
                () -> service.recordFailedPasswordAttempt(user, "user@example.com", "1.2.3.4"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(LoginAccountSecurityService.CODE_LOCKED, ex.getCode());
        assertEquals(LoginAccountSecurityService.LOCKED_SUBSCRIBED_MESSAGE, ex.getMessage());
        assertNotNull(user.getAccountLockedUntil());
        assertTrue(user.getAccountLockedUntil().isAfter(LocalDateTime.now().plusMinutes(25)));
        assertEquals(0, user.getFailedLoginAttempts());

        ArgumentCaptor<LoginSecurityEvent> eventCaptor = ArgumentCaptor.forClass(LoginSecurityEvent.class);
        verify(loginSecurityEventRepository, atLeastOnce()).save(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream()
                .anyMatch(e -> LoginSecurityEvent.ACCOUNT_LOCKED.equals(e.getEventType())));
    }

    @Test
    void unsubscribedUnverifiedLocksSilently() {
        User user = baseUser(false);
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(false);
        when(loginSecurityEventRepository.countByUserIdAndEventType(10L, LoginSecurityEvent.ACCOUNT_LOCKED))
                .thenReturn(0L);
        when(loginSecurityEventRepository.countByUserIdAndEventTypeSince(eq(10L), eq(LoginSecurityEvent.ACCOUNT_LOCKED), any()))
                .thenReturn(0L);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.recordFailedPasswordAttempt(user, "user@example.com", "1.2.3.4"));

        assertEquals(LoginAccountSecurityService.GENERIC_CREDENTIALS_MESSAGE, ex.getMessage());
        assertNotNull(user.getAccountLockedUntil());
        assertTrue(user.getAccountLockedUntil().isAfter(LocalDateTime.now().plusHours(20)));
    }

    @Test
    void unsubscribedUnverifiedSecondLockDisablesSilentlyAndEmails() {
        User user = baseUser(false);
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(false);
        when(loginSecurityEventRepository.countByUserIdAndEventType(10L, LoginSecurityEvent.ACCOUNT_LOCKED))
                .thenReturn(1L);
        when(loginSecurityEventRepository.countByUserIdAndEventTypeSince(eq(10L), eq(LoginSecurityEvent.ACCOUNT_LOCKED), any()))
                .thenReturn(0L);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.recordFailedPasswordAttempt(user, "user@example.com", "1.2.3.4"));

        assertEquals(LoginAccountSecurityService.GENERIC_CREDENTIALS_MESSAGE, ex.getMessage());
        assertTrue(user.isAccountDisabled());
        assertNotNull(user.getDisabledAt());
        verify(emailService).sendAccountDisabledEmail(user);
    }

    @Test
    void unsubscribedVerifiedDisablesOnThirdLockWithin24hSilently() {
        User user = baseUser(true);
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(false);
        when(loginSecurityEventRepository.countByUserIdAndEventTypeSince(eq(10L), eq(LoginSecurityEvent.ACCOUNT_LOCKED), any()))
                .thenReturn(2L);
        when(loginSecurityEventRepository.countByUserIdAndEventType(10L, LoginSecurityEvent.ACCOUNT_LOCKED))
                .thenReturn(2L);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.recordFailedPasswordAttempt(user, "user@example.com", "1.2.3.4"));

        assertEquals(LoginAccountSecurityService.GENERIC_CREDENTIALS_MESSAGE, ex.getMessage());
        assertTrue(user.isAccountDisabled());
        verify(emailService).sendAccountDisabledEmail(user);
    }

    @Test
    void subscribedFifthLockDisablesWithVisibleMessage() {
        User user = baseUser(true);
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(true);
        when(loginSecurityEventRepository.countByUserIdAndEventTypeSince(eq(10L), eq(LoginSecurityEvent.ACCOUNT_LOCKED), any()))
                .thenReturn(4L);
        when(loginSecurityEventRepository.countByUserIdAndEventType(10L, LoginSecurityEvent.ACCOUNT_LOCKED))
                .thenReturn(4L);

        AccountSecurityException ex = assertThrows(AccountSecurityException.class,
                () -> service.recordFailedPasswordAttempt(user, "user@example.com", "1.2.3.4"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(LoginAccountSecurityService.CODE_DISABLED, ex.getCode());
        assertEquals(LoginAccountSecurityService.DISABLED_SUBSCRIBED_MESSAGE, ex.getMessage());
        assertTrue(user.isAccountDisabled());
        verify(emailService).sendAccountDisabledEmail(user);
    }

    @Test
    void assertAllowsLoginRejectsLockedSubscribedWithMessage() {
        User user = baseUser(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(true);

        AccountSecurityException ex = assertThrows(AccountSecurityException.class,
                () -> service.assertAccountAllowsLogin(user, "user@example.com", "ip"));

        assertEquals(LoginAccountSecurityService.LOCKED_SUBSCRIBED_MESSAGE, ex.getMessage());
    }

    @Test
    void assertAllowsLoginRejectsDisabledUnsubscribedSilently() {
        User user = baseUser(false);
        user.setAccountDisabled(true);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> service.assertAccountAllowsLogin(user, "user@example.com", "ip"));

        assertEquals(LoginAccountSecurityService.GENERIC_CREDENTIALS_MESSAGE, ex.getMessage());
    }

    @Test
    void assertAllowsLoginClearsExpiredLock() {
        User user = baseUser(true);
        user.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(2);
        when(subscriptionService.hasActiveSubscription(10L)).thenReturn(false);

        assertDoesNotThrow(() -> service.assertAccountAllowsLogin(user, "user@example.com", "ip"));

        assertNull(user.getAccountLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(loginSecurityEventRepository).save(argThat(e ->
                LoginSecurityEvent.LOCK_EXPIRED_CLEARED.equals(e.getEventType())));
    }
}
