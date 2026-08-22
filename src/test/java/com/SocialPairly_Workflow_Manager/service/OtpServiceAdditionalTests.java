package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.TooManyRequestsException;
import com.SocialPairly_Workflow_Manager.repository.AuthOtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceAdditionalTests {

    @Mock
    private AuthOtpCodeRepository authOtpCodeRepository;

    private OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(authOtpCodeRepository, new BCryptPasswordEncoder());
    }

    @Test
    void shouldRejectMissingOtpWhenVerifying() {
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.verify("missing@example.com", "0000"));
    }

    @Test
    void shouldRejectExpiredOtp() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        stored.setIdentifier("user@example.com");
        stored.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        stored.setAttemptCount(0);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));
        when(authOtpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verify("user@example.com", "1234"));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectAssertVerifiedWhenOtpNotVerified() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_PASSWORD_RESET);
        stored.setIdentifier("user@example.com");
        stored.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stored.setVerified(false);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.assertVerified(AuthOtpCode.PURPOSE_PASSWORD_RESET, "user@example.com", "1234"));
        assertTrue(ex.getMessage().contains("OTP verification required"));
    }

    @Test
    void verifySucceedsAndRejectsNullOtpAsMismatch() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        stored.setIdentifier("user@example.com");
        stored.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stored.setAttemptCount(0);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));
        when(authOtpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.verify("user@example.com", "1234"));
        assertTrue(stored.getVerified());

        AuthOtpCode again = new AuthOtpCode();
        again.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        again.setIdentifier("user@example.com");
        again.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        again.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        again.setAttemptCount(0);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(again));
        assertThrows(BadRequestException.class, () -> service.verify("user@example.com", null));
    }

    @Test
    void verifyLocksAfterMaxAttemptsAndHandlesNullAttemptCount() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        stored.setIdentifier("user@example.com");
        stored.setCodeHash(new BCryptPasswordEncoder().encode("9999"));
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stored.setAttemptCount(4);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));
        when(authOtpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verify("user@example.com", "0000"));
        assertTrue(ex.getMessage().toLowerCase().contains("too many"));

        AuthOtpCode locked = new AuthOtpCode();
        locked.setAttemptCount(5);
        locked.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        locked.setCodeHash(new BCryptPasswordEncoder().encode("1"));
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(locked));
        assertThrows(BadRequestException.class, () -> service.verify("user@example.com", "1"));

        AuthOtpCode nullAttempts = new AuthOtpCode();
        nullAttempts.setAttemptCount(null);
        nullAttempts.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        nullAttempts.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(nullAttempts));
        assertDoesNotThrow(() -> service.verify("user@example.com", "1234"));
        assertEquals(1, nullAttempts.getAttemptCount());
    }

    @Test
    void assertVerifiedCoversExpiredAndMismatchAndPhoneNormalize() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_PASSWORD_RESET);
        stored.setIdentifier("+919876543210");
        stored.setCodeHash(new BCryptPasswordEncoder().encode("1234"));
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stored.setVerified(true);
        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));

        assertDoesNotThrow(() -> service.assertVerified(
                AuthOtpCode.PURPOSE_PASSWORD_RESET, "  +919876543210  ", "1234"));

        assertThrows(BadRequestException.class, () -> service.assertVerified(
                AuthOtpCode.PURPOSE_PASSWORD_RESET, "+919876543210", null));

        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThrows(BadRequestException.class, () -> service.assertVerified(
                AuthOtpCode.PURPOSE_PASSWORD_RESET, "+919876543210", "1234"));
    }

    @Test
    void generateAndClearNormalizeEmail() {
        when(authOtpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String otp = service.generateAndStore("  Ada@Example.COM ");
        assertEquals(4, otp.length());
        service.clear("Ada@Example.COM");
        verify(authOtpCodeRepository, atLeastOnce()).consumeAllActive(any(), any(), any());
    }

    @Test
    void rateLimitThrowsAfterThreshold() {
        AuthRateLimitService limiter = new AuthRateLimitService(900_000, 2, 5, 10, 10, 5);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "127.0.0.1", "a@b.com");
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "127.0.0.1", "a@b.com");
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "127.0.0.1", "a@b.com"));
    }
}
