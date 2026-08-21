package com.SocialPairly_Workflow_Manager.exception;

import org.springframework.http.HttpStatus;

/**
 * Client-facing account lock / disable responses (subscribed users only).
 * Stealth tiers use {@link org.springframework.security.authentication.BadCredentialsException} instead.
 */
public class AccountSecurityException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AccountSecurityException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
