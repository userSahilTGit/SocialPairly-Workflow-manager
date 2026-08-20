package com.SocialPairly_Workflow_Manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ConsoleSmsServiceTest {

    @Test
    void sendOtpDoesNotThrow() {
        ConsoleSmsService sms = new ConsoleSmsService();
        assertDoesNotThrow(() -> sms.sendOtp("+919876543210", "123456"));
        assertDoesNotThrow(() -> sms.sendOtp(null, null));
        assertDoesNotThrow(() -> sms.sendOtp("   ", "1"));
        assertDoesNotThrow(() -> sms.sendOtp("1234", "1"));
        assertDoesNotThrow(() -> sms.sendOtp("ab12", "1"));
    }
}
