package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberNormalizerTest {

    @Test
    void shouldNormalizeIndianNationalNumberToStorageFormat() {
        assertEquals("+91-9876543210", PhoneNumberNormalizer.toStorageFormat("9876543210", "+91"));
        assertEquals("+91-9876543210", PhoneNumberNormalizer.toStorageFormat("09876543210", "+91"));
        assertEquals("+91-9876543210", PhoneNumberNormalizer.toStorageFormat("+91-9876543210", "+91"));
    }

    @Test
    void shouldNormalizeToE164ForFirebase() {
        assertEquals("+919876543210", PhoneNumberNormalizer.toE164("9876543210", "+91"));
        assertEquals("+919876543210", PhoneNumberNormalizer.toE164("+91-9876543210", "+91"));
        assertEquals("+17372508034", PhoneNumberNormalizer.toE164("+1 737-250-8034", "+91"));
    }

    @Test
    void lookupKeysShouldIncludeStorageAndLegacyE164() {
        List<String> keys = PhoneNumberNormalizer.lookupKeys("+91-9876543210", "+91");
        assertTrue(keys.contains("+91-9876543210"));
        assertTrue(keys.contains("+919876543210"));
    }

    @Test
    void lookupKeysShouldKeepTrimmedOnlyWhenUnparseable() {
        List<String> keys = PhoneNumberNormalizer.lookupKeys("not-a-phone", "+91");
        assertEquals(List.of("not-a-phone"), keys);
    }

    @Test
    void lookupKeysShouldIgnoreBlankAndNull() {
        assertTrue(PhoneNumberNormalizer.lookupKeys(null, "+91").isEmpty());
        assertTrue(PhoneNumberNormalizer.lookupKeys("   ", "+91").isEmpty());
    }

    @Test
    void storageFormatShouldPreserveCountryCodeForLogin() {
        PhoneNumberNormalizer.PhoneParts india = PhoneNumberNormalizer.parse("+91-9876543210", "+91");
        PhoneNumberNormalizer.PhoneParts us = PhoneNumberNormalizer.parse("+1-9876543210", "+91");
        assertEquals("+91", india.countryCode());
        assertEquals("+1", us.countryCode());
        assertNotEquals(
                PhoneNumberNormalizer.toStorageFormat("+91-9876543210", "+91"),
                PhoneNumberNormalizer.toStorageFormat("+1-9876543210", "+91"));
    }

    @Test
    void parseShouldRejectNullBlankAndShortNumbers() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse(null, "+91"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("  ", "+91"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("12345", "+91"));
    }

    @Test
    void parseShouldHandleInternational00Prefix() {
        PhoneNumberNormalizer.PhoneParts parts = PhoneNumberNormalizer.parse("00919876543210", "+1");
        assertEquals("+91", parts.countryCode());
        assertEquals("9876543210", parts.national());
    }

    @Test
    void parseShouldHandleUkAndUaeCountryCodes() {
        assertEquals("+44", PhoneNumberNormalizer.parse("+447911123456", "+91").countryCode());
        assertEquals("+971", PhoneNumberNormalizer.parse("+971501234567", "+91").countryCode());
    }

    @Test
    void parseShouldFallbackWhenUnknownCountryPrefix() {
        PhoneNumberNormalizer.PhoneParts parts = PhoneNumberNormalizer.parse("+33123456789", "+91");
        assertEquals("+3", parts.countryCode());
        assertEquals("3123456789", parts.national());
    }

    @Test
    void parseShouldRejectInvalidE164Length() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.parse("+91123", "+91"));
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumberNormalizer.parse("+9112345678901234567", "+91"));
    }

    @Test
    void parseStorageFormatShouldStripLeadingZerosAndNormalizeCc() {
        PhoneNumberNormalizer.PhoneParts parts = PhoneNumberNormalizer.parse("+91-09876543210", "91");
        assertEquals("+91", parts.countryCode());
        assertEquals("9876543210", parts.national());
    }

    @Test
    void parseShouldRejectInvalidCountryCodeInStorageFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumberNormalizer.parse("9876543210", "+"));
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumberNormalizer.parse("9876543210", null));
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumberNormalizer.parse("9876543210", "   "));
    }

    @Test
    void stripLeadingZerosAndValidateNationalViaReflection() throws Exception {
        var strip = PhoneNumberNormalizer.class.getDeclaredMethod("stripLeadingZeros", String.class);
        strip.setAccessible(true);
        assertEquals("", strip.invoke(null, (Object) null));
        assertEquals("9876543210", strip.invoke(null, "0009876543210"));

        var validate = PhoneNumberNormalizer.class.getDeclaredMethod("validateNational", String.class);
        validate.setAccessible(true);
        Exception ex = assertThrows(Exception.class, () -> validate.invoke(null, (Object) null));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void parseShouldNormalizeCountryCodeWithoutPlusPrefix() {
        PhoneNumberNormalizer.PhoneParts parts = PhoneNumberNormalizer.parse("9876543210", "91");
        assertEquals("+91", parts.countryCode());
        assertEquals("9876543210", parts.national());
    }

    @Test
    void parseShouldRejectNationalTooLong() {
        assertThrows(IllegalArgumentException.class,
                () -> PhoneNumberNormalizer.parse("+91-1234567890123456", "+91"));
    }
}
