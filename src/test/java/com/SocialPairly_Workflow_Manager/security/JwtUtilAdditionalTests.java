package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilAdditionalTests {

    @Test
    void shouldRejectInvalidToken() {
        JwtUtil jwtUtil = new JwtUtil("this-is-a-very-long-secret-key-for-jwt-123456", 1000L);
        assertFalse(jwtUtil.isTokenValid("invalid-token"));
    }
}
