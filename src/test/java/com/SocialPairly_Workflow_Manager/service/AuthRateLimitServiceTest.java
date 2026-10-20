package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitServiceTest {

    @Test
    void checkAllowsUpToMaxThenThrows() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 2, 5, 10, 10, 5, 5, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "ada@example.com");
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "ada@example.com");
        TooManyRequestsException ex = assertThrows(
                TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "ada@example.com"));
        assertEquals("Too many attempts. Try again later.", ex.getMessage());
    }

    @Test
    void checkTreatsBlankIpAndIdentifierAsUnknown() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 1, 5, 10, 10, 5, 5, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "  ", null);
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_LOGIN, null, "   "));
    }

    @Test
    void checkIsCaseInsensitiveOnIdentifier() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 1, 5, 10, 10, 5, 5, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "10.0.0.1", "Ada@Example.com");
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "10.0.0.1", "ada@example.com"));
    }

    @Test
    void unknownActionIsSkipped() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 1, 5, 10, 10, 5, 5, 5, 10);
        assertDoesNotThrow(() -> limiter.check("unknown-action", "1.1.1.1", "x"));
        assertDoesNotThrow(() -> limiter.check("unknown-action", "1.1.1.1", "x"));
    }

    @Test
    void zeroMaxDisablesLimit() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 0, 5, 10, 10, 5, 5, 5, 10);
        assertDoesNotThrow(() -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "a"));
        assertDoesNotThrow(() -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "a"));
    }

    @Test
    void expiredEntriesArePrunedFromTheWindow() throws InterruptedException {
        AuthRateLimitService limiter = new AuthRateLimitService(20L, 1, 5, 10, 10, 5, 5, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "2.2.2.2", "ada");
        Thread.sleep(40);
        assertDoesNotThrow(() -> limiter.check(AuthRateLimitService.ACTION_LOGIN, "2.2.2.2", "ada"));
    }

    @Test
    void differentActionsHaveSeparateBuckets() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 1, 1, 10, 10, 5, 5, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_LOGIN, "1.1.1.1", "ada");
        assertDoesNotThrow(() -> limiter.check(AuthRateLimitService.ACTION_FORGOT_SEND, "1.1.1.1", "ada"));
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_FORGOT_SEND, "1.1.1.1", "ada"));
    }

    @Test
    void registerActionIsRateLimited() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 10, 5, 10, 10, 5, 2, 5, 10);
        limiter.check(AuthRateLimitService.ACTION_REGISTER, "1.1.1.1", "new@example.com");
        limiter.check(AuthRateLimitService.ACTION_REGISTER, "1.1.1.1", "new@example.com");
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_REGISTER, "1.1.1.1", "new@example.com"));
    }

    @Test
    void deleteSendOtpAndVerifyAreRateLimitedSeparately() {
        AuthRateLimitService limiter = new AuthRateLimitService(60_000L, 10, 5, 10, 10, 5, 5, 1, 1);
        limiter.check(AuthRateLimitService.ACTION_DELETE_SEND_OTP, "1.1.1.1", "11");
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_DELETE_SEND_OTP, "1.1.1.1", "11"));
        assertDoesNotThrow(() -> limiter.check(AuthRateLimitService.ACTION_DELETE_VERIFY, "1.1.1.1", "11"));
        assertThrows(TooManyRequestsException.class,
                () -> limiter.check(AuthRateLimitService.ACTION_DELETE_VERIFY, "1.1.1.1", "11"));
    }
}
