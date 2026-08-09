package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.entity.AuthOtpCode;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.repository.AuthOtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private AuthOtpCodeRepository authOtpCodeRepository;

    private PasswordEncoder passwordEncoder;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        otpService = new OtpService(authOtpCodeRepository, passwordEncoder);
        lenient().when(authOtpCodeRepository.consumeAllActive(any(), any(), any())).thenReturn(0);
        lenient().when(authOtpCodeRepository.save(any(AuthOtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldGenerateAndVerifyOtp() {
        String identifier = "user@example.com";
        String otp = otpService.generateAndStore(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier);
        assertNotNull(otp);
        assertEquals(4, otp.length());

        ArgumentCaptor<AuthOtpCode> captor = ArgumentCaptor.forClass(AuthOtpCode.class);
        verify(authOtpCodeRepository, atLeastOnce()).save(captor.capture());
        AuthOtpCode stored = captor.getValue();

        when(authOtpCodeRepository.findLatestActive(eq(AuthOtpCode.PURPOSE_EMAIL_VERIFY), eq("user@example.com"), any()))
                .thenReturn(Optional.of(stored));

        otpService.verify(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier, otp);
        assertTrue(Boolean.TRUE.equals(stored.getVerified()));

        assertDoesNotThrow(() -> otpService.assertVerified(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier, otp));

        otpService.clear(AuthOtpCode.PURPOSE_EMAIL_VERIFY, identifier);
        verify(authOtpCodeRepository, atLeastOnce())
                .consumeAllActive(eq(AuthOtpCode.PURPOSE_EMAIL_VERIFY), eq("user@example.com"), any());
    }

    @Test
    void shouldRejectInvalidOtp() {
        AuthOtpCode stored = new AuthOtpCode();
        stored.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        stored.setIdentifier("user@example.com");
        stored.setCodeHash(passwordEncoder.encode("1234"));
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stored.setAttemptCount(0);
        stored.setVerified(false);

        when(authOtpCodeRepository.findLatestActive(any(), any(), any())).thenReturn(Optional.of(stored));

        assertThrows(BadRequestException.class,
                () -> otpService.verify(AuthOtpCode.PURPOSE_EMAIL_VERIFY, "user@example.com", "9999"));
        assertEquals(1, stored.getAttemptCount());
    }
}
