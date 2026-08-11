package com.SocialPairly_Workflow_Manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTokenServiceTest {

    @Test
    void parsePlanTokensShouldHandleNumericAndFormattedValues() {
        assertEquals(5000, UserTokenService.parsePlanTokens("5,000"));
        assertEquals(100, UserTokenService.parsePlanTokens("100"));
        assertEquals(0, UserTokenService.parsePlanTokens("UNLIMITED"));
        assertEquals(0, UserTokenService.parsePlanTokens(null));
        assertEquals(0, UserTokenService.parsePlanTokens(""));
        assertEquals(50, UserTokenService.DEFAULT_NEW_USER_TOKENS);
    }
}
