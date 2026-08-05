package com.SocialPairly_Workflow_Manager.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextModerationServiceTest {
    private final TextModerationService service = new TextModerationService();

    @Test
    @DisplayName("containsForbiddenText - null -> false")
    void nullText_false() {
        assertFalse(service.containsForbiddenText(null));
    }

    @Test
    @DisplayName("containsForbiddenText - blank -> false")
    void blankText_false() {
        assertFalse(service.containsForbiddenText("   "));
    }

    @Test
    @DisplayName("containsForbiddenText - normal text -> false")
    void cleanText_false() {
        assertFalse(service.containsForbiddenText("Hello, I love hiking and photography!"));
    }

    @Test
    @DisplayName("containsForbiddenText - email address -> true")
    void email_true() {
        assertTrue(service.containsForbiddenText("Contact me at test@example.com"));
    }

    @Test
    @DisplayName("containsForbiddenText - URL -> true")
    void url_true() {
        assertTrue(service.containsForbiddenText("Check https://example.com"));
    }

    @Test
    @DisplayName("containsForbiddenText - www URL -> true")
    void wwwUrl_true() {
        assertTrue(service.containsForbiddenText("Visit www.spam.com now"));
    }

    @Test
    @DisplayName("containsForbiddenText - handle @ -> true")
    void handle_true() {
        assertTrue(service.containsForbiddenText("Follow @spambot please"));
    }

    @Test
    @DisplayName("containsForbiddenText - insta reference -> true")
    void insta_true() {
        assertTrue(service.containsForbiddenText("My insta: photos4u"));
    }

    @Test
    @DisplayName("containsForbiddenText - phone number -> true")
    void phone_true() {
        assertTrue(service.containsForbiddenText("Call +12345678901 now"));
    }

    @Test
    @DisplayName("containsForbiddenText - case insensitive email -> true")
    void upperCaseEmail_true() {
        assertTrue(service.containsForbiddenText("EMAIL: TEST@EXAMPLE.COM"));
    }

    @Test
    @DisplayName("containsForbiddenText - mixed case handle -> true")
    void mixedCaseHandle_true() {
        assertTrue(service.containsForbiddenText("Hey @SpamBoi"));
    }
}
