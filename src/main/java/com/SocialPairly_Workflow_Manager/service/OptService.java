package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final long OTP_TTL_MS = 10 * 60 * 1000; // 10 minutes

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public String generateAndStore(String identifier) {
        String key = normalize(identifier);
        String otp = String.format("%04d", random.nextInt(10000));
        store.put(key, new OtpEntry(otp, Instant.now().toEpochMilli() + OTP_TTL_MS, false));
        log.info("OTP for {}: {} (valid for 10 minutes)", key, otp);
        return otp;
    }

    public void verify(String identifier, String otp) {
        String key = normalize(identifier);
        OtpEntry entry = store.get(key);
        if (entry == null) {
            throw new BadRequestException("No OTP found. Please request a new one.");
        }
        if (Instant.now().toEpochMilli() > entry.expiresAt) {
            store.remove(key);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!entry.otp.equals(otp.trim())) {
            throw new BadRequestException("Invalid OTP");
        }
        entry.verified = true;
    }

    public void assertVerified(String identifier, String otp) {
        String key = normalize(identifier);
        OtpEntry entry = store.get(key);
        if (entry == null || !entry.verified || !entry.otp.equals(otp.trim())) {
            throw new BadRequestException("OTP verification required before resetting password");
        }
        if (Instant.now().toEpochMilli() > entry.expiresAt) {
            store.remove(key);
            throw new BadRequestException("OTP has expired. Please start over.");
        }
    }

    public void clear(String identifier) {
        store.remove(normalize(identifier));
    }

    private String normalize(String identifier) {
        String trimmed = identifier.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : trimmed;
    }

    private static class OtpEntry {
        final String otp;
        final long expiresAt;
        boolean verified;

        OtpEntry(String otp, long expiresAt, boolean verified) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.verified = verified;
        }
    }
}