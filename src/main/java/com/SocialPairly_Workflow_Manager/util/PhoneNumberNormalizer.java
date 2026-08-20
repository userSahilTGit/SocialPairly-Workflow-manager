package com.SocialPairly_Workflow_Manager.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Normalizes phone numbers for DB storage ({@code +91-9876543210}) and external APIs (E.164).
 */
public final class PhoneNumberNormalizer {

    /** Longest-prefix first so +971 is matched before +91 and +1. */
    private static final String[] KNOWN_COUNTRY_CODES = {"+971", "+91", "+44", "+1"};

    private PhoneNumberNormalizer() {}

    public record PhoneParts(String countryCode, String national) {}

    /**
     * Canonical DB format: CountryCode-PhoneNumber (e.g. {@code +91-9876543210}).
     */
    public static String toStorageFormat(String raw, String defaultCountryCode) {
        PhoneParts parts = parse(raw, defaultCountryCode);
        return formatStorage(parts);
    }

    /** E.164 without hyphen — for Firebase SMS and similar APIs. */
    public static String toE164(String raw, String defaultCountryCode) {
        PhoneParts parts = parse(raw, defaultCountryCode);
        return parts.countryCode() + parts.national();
    }

    /** Candidate DB keys for lookup (storage format + legacy E.164 + raw). */
    public static List<String> lookupKeys(String raw, String defaultCountryCode) {
        Set<String> keys = new LinkedHashSet<>();
        String trimmed = raw == null ? "" : raw.trim();
        if (!trimmed.isBlank()) {
            keys.add(trimmed);
        }
        try {
            PhoneParts parts = parse(raw, defaultCountryCode);
            keys.add(formatStorage(parts));
            keys.add(parts.countryCode() + parts.national());
        } catch (IllegalArgumentException ignored) {
            // keep trimmed only
        }
        return new ArrayList<>(keys);
    }

    public static PhoneParts parse(String raw, String defaultCountryCode) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String trimmed = raw.trim();

        if (trimmed.matches("^\\+\\d{1,4}-.+")) {
            int dash = trimmed.indexOf('-');
            String ccPart = trimmed.substring(0, dash).replaceAll("[^0-9+]", "");
            String national = trimmed.substring(dash + 1).replaceAll("\\D", "");
            national = stripLeadingZeros(national);
            String cc = normalizeCountryCode(ccPart, defaultCountryCode);
            validateNational(national);
            return new PhoneParts(cc, national);
        }

        String digitsOnly = trimmed.replaceAll("[^0-9+]", "");
        if (digitsOnly.startsWith("00")) {
            digitsOnly = "+" + digitsOnly.substring(2);
        }

        if (digitsOnly.startsWith("+")) {
            PhoneParts split = splitE164(digitsOnly);
            validateNational(split.national());
            return split;
        }

        String national = stripLeadingZeros(digitsOnly.replaceAll("\\D", ""));
        String cc = normalizeCountryCode(defaultCountryCode, defaultCountryCode);
        validateNational(national);
        return new PhoneParts(cc, national);
    }

    private static String formatStorage(PhoneParts parts) {
        return parts.countryCode() + "-" + parts.national();
    }

    private static PhoneParts splitE164(String e164) {
        String body = e164.substring(1).replaceAll("\\D", "");
        for (String code : KNOWN_COUNTRY_CODES) {
            String ccDigits = code.substring(1);
            if (body.startsWith(ccDigits)) {
                String national = body.substring(ccDigits.length());
                if (national.length() >= 8 && national.length() <= 15) {
                    return new PhoneParts(code, national);
                }
            }
        }
        if (body.length() < 9 || body.length() > 16) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
        // Fallback: treat first digit as +1-style country code
        return new PhoneParts("+" + body.charAt(0), body.substring(1));
    }

    private static String normalizeCountryCode(String cc, String fallback) {
        String source = (cc == null || cc.isBlank()) ? fallback : cc;
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Invalid country code");
        }
        source = source.trim();
        if (!source.startsWith("+")) {
            source = "+" + source.replaceAll("\\D", "");
        } else {
            source = "+" + source.substring(1).replaceAll("\\D", "");
        }
        if (source.equals("+") || source.length() < 2) {
            throw new IllegalArgumentException("Invalid country code");
        }
        return source;
    }

    private static String stripLeadingZeros(String national) {
        if (national == null) {
            return "";
        }
        return national.replaceFirst("^0+", "");
    }

    private static void validateNational(String national) {
        if (national == null || national.length() < 8 || national.length() > 15) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
    }
}
