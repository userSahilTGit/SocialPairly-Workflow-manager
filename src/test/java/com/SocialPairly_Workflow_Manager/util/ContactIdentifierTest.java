package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactIdentifierTest {

    @Test
    void acceptsValidEmail() {
        assertTrue(ContactIdentifier.isValidEmailOrPhone("ada@example.com"));
        assertTrue(ContactIdentifier.isValidEmailOrPhone("  Ada.Lovelace+test@Example.COM "));
    }

    @Test
    void rejectsMalformedEmail() {
        assertFalse(ContactIdentifier.isValidEmailOrPhone("not-an-email"));
        assertFalse(ContactIdentifier.isValidEmailOrPhone("ada@"));
        assertFalse(ContactIdentifier.isValidEmailOrPhone("@example.com"));
    }

    @Test
    void acceptsValidPhone() {
        assertTrue(ContactIdentifier.isValidEmailOrPhone("9876543210"));
        assertTrue(ContactIdentifier.isValidEmailOrPhone("+91-9876543210"));
        assertTrue(ContactIdentifier.isValidEmailOrPhone("+12025550123"));
    }

    @Test
    void rejectsInvalidPhoneAndBlank() {
        assertFalse(ContactIdentifier.isValidEmailOrPhone("123"));
        assertFalse(ContactIdentifier.isValidEmailOrPhone(""));
        assertFalse(ContactIdentifier.isValidEmailOrPhone(null));
        assertFalse(ContactIdentifier.isValidEmailOrPhone("   "));
    }
}
