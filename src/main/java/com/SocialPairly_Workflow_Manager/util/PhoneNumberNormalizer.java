package com.SocialPairly_Workflow_Manager.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normalizes phone numbers for DB storage ({@code +91-9876543210}) and external APIs (E.164).
 * National lengths are strict per dial code when registered (e.g. India = exactly 10).
 */
public final class PhoneNumberNormalizer {

    private static final Map<String, Integer> NATIONAL_LENGTH_BY_DIAL = new ConcurrentHashMap<>();

    /** Fallback longest-prefix list until worldwide codes are registered at startup. */
    private static volatile String[] knownCountryCodes = {"+971", "+91", "+44", "+1"};

    static {
        NATIONAL_LENGTH_BY_DIAL.put("+1", 10);
        NATIONAL_LENGTH_BY_DIAL.put("+91", 10);
        NATIONAL_LENGTH_BY_DIAL.put("+44", 10);
        NATIONAL_LENGTH_BY_DIAL.put("+971", 9);
    }

    private PhoneNumberNormalizer() {}

    public record PhoneParts(String countryCode, String national) {}

    public static void registerNationalLength(String dialCode, int nationalLength) {
        if (dialCode == null || dialCode.isBlank() || nationalLength < 4 || nationalLength > 15) {
            return;
        }
        String cc = normalizeCountryCode(dialCode, dialCode);
        NATIONAL_LENGTH_BY_DIAL.put(cc, nationalLength);
        refreshKnownCountryCodes();
    }

    public static Integer nationalLengthFor(String dialCode) {
        if (dialCode == null || dialCode.isBlank()) {
            return null;
        }
        try {
            return NATIONAL_LENGTH_BY_DIAL.get(normalizeCountryCode(dialCode, dialCode));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

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
            validateNational(national, cc);
            return new PhoneParts(cc, national);
        }

        String digitsOnly = trimmed.replaceAll("[^0-9+]", "");
        if (digitsOnly.startsWith("00")) {
            digitsOnly = "+" + digitsOnly.substring(2);
        }

        if (digitsOnly.startsWith("+")) {
            PhoneParts split = splitE164(digitsOnly);
            validateNational(split.national(), split.countryCode());
            return split;
        }

        String national = stripLeadingZeros(digitsOnly.replaceAll("\\D", ""));
        String cc = normalizeCountryCode(defaultCountryCode, defaultCountryCode);
        validateNational(national, cc);
        return new PhoneParts(cc, national);
    }

    private static String formatStorage(PhoneParts parts) {
        return parts.countryCode() + "-" + parts.national();
    }

    private static PhoneParts splitE164(String e164) {
        String body = e164.substring(1).replaceAll("\\D", "");
        for (String code : knownCountryCodes) {
            String ccDigits = code.substring(1);
            if (body.startsWith(ccDigits)) {
                String national = body.substring(ccDigits.length());
                Integer expected = NATIONAL_LENGTH_BY_DIAL.get(code);
                if (expected != null) {
                    if (national.length() == expected) {
                        return new PhoneParts(code, national);
                    }
                    continue;
                }
                if (national.length() >= 8 && national.length() <= 15) {
                    return new PhoneParts(code, national);
                }
            }
        }
        if (body.length() < 9 || body.length() > 16) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
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

    private static void validateNational(String national, String dialCode) {
        if (national == null || national.isBlank()) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
        Integer expected = NATIONAL_LENGTH_BY_DIAL.get(dialCode);
        if (expected != null) {
            if (national.length() != expected) {
                throw new IllegalArgumentException(
                        "Phone number must be exactly " + expected + " digits for " + dialCode);
            }
            return;
        }
        if (national.length() < 8 || national.length() > 15) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
    }

    private static void refreshKnownCountryCodes() {
        knownCountryCodes = NATIONAL_LENGTH_BY_DIAL.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(s -> s))
                .toArray(String[]::new);
    }
}
