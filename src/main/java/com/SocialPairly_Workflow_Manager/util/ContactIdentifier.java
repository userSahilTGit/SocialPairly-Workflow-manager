package com.SocialPairly_Workflow_Manager.util;

import java.util.regex.Pattern;

/**
 * Shared validation for login / forgot-password identifiers (email or phone).
 */
public final class ContactIdentifier {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Fallback country code only for phone-shape validation (not storage). */
    private static final String VALIDATION_DEFAULT_CC = "+91";

    private ContactIdentifier() {}

    public static boolean isValidEmailOrPhone(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return EMAIL.matcher(trimmed).matches();
        }
        try {
            PhoneNumberNormalizer.parse(trimmed, VALIDATION_DEFAULT_CC);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
