package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenBlacklistServiceTest {

    @Test
    void revokeShouldMarkTokenAsRevokedUntilExpiry() {
        JwtTokenBlacklistService service = new JwtTokenBlacklistService();
        String token = "logout-jwt-token";
        Date expiry = new Date(System.currentTimeMillis() + 60_000);

        assertFalse(service.isRevoked(token));
        service.revoke(token, expiry);
        assertTrue(service.isRevoked(token));
    }

    @Test
    void isRevokedShouldReturnFalseAfterExpiry() {
        JwtTokenBlacklistService service = new JwtTokenBlacklistService();
        String token = "expired-jwt";
        service.revoke(token, new Date(System.currentTimeMillis() - 1_000));
        assertFalse(service.isRevoked(token));
    }

    @Test
    void revokeShouldIgnoreBlankToken() {
        JwtTokenBlacklistService service = new JwtTokenBlacklistService();
        service.revoke(" ", new Date());
        assertFalse(service.isRevoked(" "));
    }
}
