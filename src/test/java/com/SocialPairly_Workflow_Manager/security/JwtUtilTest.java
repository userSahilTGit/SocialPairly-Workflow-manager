package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("01234567890123456789012345678901", 3_600_000L, 2_592_000_000L);
    }

    @Test
    void generateTokenWithoutRememberMeUsesDefaultTtl() {
        String token = jwtUtil.generateToken("ada@example.com");
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("ada@example.com", jwtUtil.extractEmail(token));
        Date expiry = jwtUtil.extractExpiration(token);
        long delta = expiry.getTime() - System.currentTimeMillis();
        assertTrue(delta > 3_000_000L && delta <= 3_600_000L + 5_000L);
    }

    @Test
    void generateTokenWithRememberMeUsesLongerTtl() {
        String token = jwtUtil.generateToken("ada@example.com", true);
        assertTrue(jwtUtil.isTokenValid(token));
        Date expiry = jwtUtil.extractExpiration(token);
        long delta = expiry.getTime() - System.currentTimeMillis();
        assertTrue(delta > 2_000_000_000L);
    }

    @Test
    void generateTokenWithRememberMeFalseMatchesDefault() {
        String token = jwtUtil.generateToken("bob@example.com", false);
        assertEquals("bob@example.com", jwtUtil.extractEmail(token));
        assertEquals(3_600_000L, jwtUtil.resolveExpirationMs(false));
        assertEquals(2_592_000_000L, jwtUtil.resolveExpirationMs(true));
    }

    @Test
    void isTokenValidReturnsFalseForGarbage() {
        assertFalse(jwtUtil.isTokenValid("not-a-jwt"));
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    void isTokenValidReturnsFalseForTamperedToken() {
        String token = jwtUtil.generateToken("ada@example.com");
        assertFalse(jwtUtil.isTokenValid(token + "x"));
    }
}
