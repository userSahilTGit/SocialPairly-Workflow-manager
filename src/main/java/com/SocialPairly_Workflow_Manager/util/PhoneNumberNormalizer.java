package com.SocialPairly_Workflow_Manager.util;

/**
 * Normalizes phone numbers to E.164 for consistent DB storage and Firebase Auth.
 */
public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {}

    /**
     * @param raw                 user-entered phone
     * @param defaultCountryCode  e.g. "+91" used when input has no country prefix
     */
    public static String toE164(String raw, String defaultCountryCode) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String trimmed = raw.trim();
        String digitsOnly = trimmed.replaceAll("[^0-9+]", "");

        if (digitsOnly.startsWith("00")) {
            digitsOnly = "+" + digitsOnly.substring(2);
        }

        if (digitsOnly.startsWith("+")) {
            String national = digitsOnly.substring(1).replaceAll("\\D", "");
            if (national.length() < 8 || national.length() > 15) {
                throw new IllegalArgumentException("Invalid phone number length");
            }
            return "+" + national;
        }

        String national = digitsOnly.replaceAll("\\D", "");
        // Drop leading 0 from national numbers (e.g. 09876... -> 9876...)
        if (national.startsWith("0")) {
            national = national.replaceFirst("^0+", "");
        }

        String cc = defaultCountryCode == null || defaultCountryCode.isBlank()
                ? "+91"
                : defaultCountryCode.trim();
        if (!cc.startsWith("+")) {
            cc = "+" + cc.replaceAll("\\D", "");
        } else {
            cc = "+" + cc.substring(1).replaceAll("\\D", "");
        }

        if (national.length() < 8 || national.length() > 12) {
            throw new IllegalArgumentException("Invalid phone number length");
        }
        return cc + national;
    }
}
