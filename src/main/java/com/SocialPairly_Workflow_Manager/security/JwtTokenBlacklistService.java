package com.SocialPairly_Workflow_Manager.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node JWT revocation list (logout invalidation).
 * Multi-instance deployments should replace this with a shared store (e.g. Redis).
 */
@Service
public class JwtTokenBlacklistService {

    private final ConcurrentHashMap<String, Long> revokedUntilMs = new ConcurrentHashMap<>();

    public void revoke(String token, Date expiresAt) {
        if (token == null || token.isBlank()) {
            return;
        }
        long until = expiresAt != null ? expiresAt.getTime() : System.currentTimeMillis();
        revokedUntilMs.put(fingerprint(token), until);
        purgeExpired();
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = fingerprint(token);
        Long until = revokedUntilMs.get(key);
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            revokedUntilMs.remove(key, until);
            return false;
        }
        return true;
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = revokedUntilMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getValue() <= now) {
                it.remove();
            }
        }
    }

    static String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on supported JDKs
            return Integer.toHexString(token.hashCode());
        }
    }
}
