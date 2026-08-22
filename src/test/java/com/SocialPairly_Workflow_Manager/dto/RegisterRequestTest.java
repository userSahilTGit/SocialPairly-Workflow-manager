package com.SocialPairly_Workflow_Manager.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void assertTrueHelpersCoverPassAndFailBranches() {
        RegisterRequest ok = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "+919876543210",
                "secret1", "secret1", "addr",
                true, true, true, true, false
        );
        assertTrue(ok.isPasswordConfirmed());
        assertTrue(ok.isAgeConfirmed());
        assertTrue(ok.isTermsConfirmed());
        assertTrue(ok.isPrivacyConfirmed());
        assertTrue(ok.isIdentityConfirmed());

        RegisterRequest bad = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "+919876543210",
                "secret1", "other", null,
                false, false, false, false, true
        );
        assertFalse(bad.isPasswordConfirmed());
        assertFalse(bad.isAgeConfirmed());
        assertFalse(bad.isTermsConfirmed());
        assertFalse(bad.isPrivacyConfirmed());
        assertFalse(bad.isIdentityConfirmed());
    }

    @Test
    void passwordConfirmedFalseWhenPasswordNull() {
        RegisterRequest req = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "+919876543210",
                null, "secret1", null,
                true, true, true, true, false
        );
        assertFalse(req.isPasswordConfirmed());
    }
}
