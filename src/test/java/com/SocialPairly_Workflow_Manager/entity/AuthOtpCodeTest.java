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
        LocalDateTime created = LocalDateTime.now().minusMinutes(1);
        code.setId(9L);
        code.setPurpose(AuthOtpCode.PURPOSE_ACCOUNT_DELETE);
        code.setIdentifier("member@example.com");
        code.setCodeHash("hash");
        code.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        code.setConsumedAt(LocalDateTime.now());
        code.setCreatedAt(created);

        assertEquals(9L, code.getId());
        assertEquals(AuthOtpCode.PURPOSE_ACCOUNT_DELETE, code.getPurpose());
        assertEquals("member@example.com", code.getIdentifier());
        assertEquals("hash", code.getCodeHash());
        assertNotNull(code.getExpiresAt());
        assertNotNull(code.getConsumedAt());
        assertEquals(created, code.getCreatedAt());
        assertEquals("ACCOUNT_DELETE", AuthOtpCode.PURPOSE_ACCOUNT_DELETE);
    }
}
