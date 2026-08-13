package com.SocialPairly_Workflow_Manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ConsoleSmsServiceTest {

    @Test
    void sendOtpDoesNotThrow() {
        ConsoleSmsService sms = new ConsoleSmsService();
        assertDoesNotThrow(() -> sms.sendOtp("+919876543210", "123456"));
        assertDoesNotThrow(() -> sms.sendOtp(null, null));
    }
}
