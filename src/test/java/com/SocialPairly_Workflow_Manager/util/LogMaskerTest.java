package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogMaskerTest {

    @Test
    void masksEmailLocalPart() {
        assertEquals("j***@example.com", LogMasker.email("john.doe@example.com"));
        assertEquals("***", LogMasker.email("@example.com"));
        assertEquals("***", LogMasker.email("not-an-email"));
        assertEquals("***", LogMasker.email(null));
    }

    @Test
    void keepsOnlyLastFourPhoneDigits() {
        assertEquals("***3210", LogMasker.phone("+91-9876543210"));
        assertEquals("***", LogMasker.phone("12"));
        assertEquals("***", LogMasker.phone(" "));
    }

    @Test
    void picksFormatFromIdentifierShape() {
        assertEquals("a***@socialpairly.com", LogMasker.identifier("admin@socialpairly.com"));
        assertEquals("***3210", LogMasker.identifier("+919876543210"));
        assertEquals("***", LogMasker.identifier(null));
    }
}
