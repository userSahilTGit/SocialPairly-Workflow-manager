package com.SocialPairly_Workflow_Manager.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

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
        service.revoke(null, null);
        assertFalse(service.isRevoked(" "));
        assertFalse(service.isRevoked(null));
        assertFalse(service.isRevoked(""));
    }

    @Test
    void expiredEntryIsRemovedWhenChecked() throws Exception {
        JwtTokenBlacklistService service = new JwtTokenBlacklistService();
        String token = "still-listed";
        service.revoke(token, new Date(System.currentTimeMillis() + 60_000));

        Field field = JwtTokenBlacklistService.class.getDeclaredField("revokedUntilMs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> map = (ConcurrentHashMap<String, Long>) field.get(service);
        map.replaceAll((key, until) -> System.currentTimeMillis() - 1_000);

        assertFalse(service.isRevoked(token));
        assertFalse(service.isRevoked(token));
    }
}
