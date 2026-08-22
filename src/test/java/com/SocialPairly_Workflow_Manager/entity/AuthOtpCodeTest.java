package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuthOtpCodeTest {

    @Test
    void onCreateInitializesNullVerifiedAndAttemptCount() {
        AuthOtpCode code = new AuthOtpCode();
        code.setPurpose(AuthOtpCode.PURPOSE_EMAIL_VERIFY);
        code.setIdentifier("a@b.com");
        code.setCodeHash("hash");
        code.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        code.setVerified(null);
        code.setAttemptCount(null);

        code.onCreate();

        assertNotNull(code.getCreatedAt());
        assertEquals(Boolean.FALSE, code.getVerified());
        assertEquals(0, code.getAttemptCount());
    }

    @Test
    void onCreatePreservesExistingVerifiedAndAttempts() {
        AuthOtpCode code = new AuthOtpCode();
        code.setPurpose(AuthOtpCode.PURPOSE_PASSWORD_RESET);
        code.setIdentifier("a@b.com");
        code.setCodeHash("hash");
        code.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        code.setVerified(true);
        code.setAttemptCount(3);

        code.onCreate();

        assertTrue(code.getVerified());
        assertEquals(3, code.getAttemptCount());
        assertNotNull(code.getCreatedAt());
    }

    @Test
    void accessorsRoundTrip() {
        AuthOtpCode code = new AuthOtpCode();
        code.setId(9L);
        code.setConsumedAt(LocalDateTime.now());
        assertEquals(9L, code.getId());
        assertNotNull(code.getConsumedAt());
        assertEquals(AuthOtpCode.PURPOSE_EMAIL_VERIFY, AuthOtpCode.PURPOSE_EMAIL_VERIFY);
    }
}
