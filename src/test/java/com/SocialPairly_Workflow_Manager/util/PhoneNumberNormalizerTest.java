package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberNormalizerTest {

    @Test
    void shouldNormalizeIndianNationalNumber() {
        assertEquals("+919876543210", PhoneNumberNormalizer.toE164("9876543210", "+91"));
        assertEquals("+919876543210", PhoneNumberNormalizer.toE164("09876543210", "+91"));
    }

    @Test
    void shouldKeepExplicitE164() {
        assertEquals("+17372508034", PhoneNumberNormalizer.toE164("+1 737-250-8034", "+91"));
    }
}
