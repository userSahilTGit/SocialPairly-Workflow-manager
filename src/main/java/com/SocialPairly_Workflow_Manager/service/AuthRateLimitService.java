package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node sliding-window rate limiter for auth endpoints.
 * Multi-instance deployments should replace this with Redis.
 */
@Service
public class AuthRateLimitService {

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_FORGOT_SEND = "forgot-send";
    public static final String ACTION_FORGOT_VERIFY = "forgot-verify";
    public static final String ACTION_VERIFY_EMAIL = "verify-email";
    public static final String ACTION_RESEND_VERIFICATION = "resend-verification";

    private final long windowMs;
    private final Map<String, Integer> limits;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitService(
            @Value("${app.rate-limit.window-ms:900000}") long windowMs,
            @Value("${app.rate-limit.login-max:10}") int loginMax,
            @Value("${app.rate-limit.forgot-send-max:5}") int forgotSendMax,
            @Value("${app.rate-limit.forgot-verify-max:10}") int forgotVerifyMax,
            @Value("${app.rate-limit.verify-email-max:10}") int verifyEmailMax,
            @Value("${app.rate-limit.resend-verification-max:5}") int resendMax,
            @Value("${app.rate-limit.register-max:5}") int registerMax
    ) {
        this.windowMs = windowMs;
        this.limits = Map.of(
                ACTION_LOGIN, loginMax,
                ACTION_REGISTER, registerMax,
                ACTION_FORGOT_SEND, forgotSendMax,
                ACTION_FORGOT_VERIFY, forgotVerifyMax,
                ACTION_VERIFY_EMAIL, verifyEmailMax,
                ACTION_RESEND_VERIFICATION, resendMax
        );
    }

    public void check(String action, String clientIp, String identifier) {
        Integer max = limits.get(action);
        if (max == null || max <= 0) {
            return;
        }
        String key = action + "|" + normalize(clientIp) + "|" + normalize(identifier);
        long now = System.currentTimeMillis();
        Deque<Long> times = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > windowMs) {
                times.pollFirst();
            }
            if (times.size() >= max) {
                throw new TooManyRequestsException("Too many attempts. Try again later.");
            }
            times.addLast(now);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}
