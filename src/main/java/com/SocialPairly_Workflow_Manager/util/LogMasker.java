package com.SocialPairly_Workflow_Manager.util;

/**
 * Masks personal data before it is written to logs (defect 1584).
 * Emails keep the first character and the domain; phone numbers keep the last 4 digits.
 */
public final class LogMasker {

    private static final String MASK = "***";

    private LogMasker() {}

    /** Masks an email or phone number, choosing the format from the value's shape. */
    public static String identifier(String value) {
        if (value == null || value.isBlank()) {
            return MASK;
        }
        return value.contains("@") ? email(value) : phone(value);
    }

    public static String email(String value) {
        if (value == null || value.isBlank()) {
            return MASK;
        }
        String trimmed = value.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return MASK;
        }
        return trimmed.charAt(0) + MASK + trimmed.substring(at);
    }

    public static String phone(String value) {
        if (value == null || value.isBlank()) {
            return MASK;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return MASK;
        }
        return MASK + digits.substring(digits.length() - 4);
    }
}
