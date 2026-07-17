package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {

    private final OtpService otpService = new OtpService();

    @Test
    void shouldGenerateAndVerifyOtp() {
        String identifier = "user@example.com";

        String otp = otpService.generateAndStore(identifier);

        assertNotNull(otp);
        assertEquals(4, otp.length());

        otpService.verify(identifier, otp);
        assertDoesNotThrow(() -> otpService.assertVerified(identifier, otp));

        otpService.clear(identifier);
        assertThrows(BadRequestException.class, () -> otpService.assertVerified(identifier, otp));
    }

    @Test
    void shouldRejectInvalidOtp() {
        String identifier = "user@example.com";
        otpService.generateAndStore(identifier);

        assertThrows(BadRequestException.class, () -> otpService.verify(identifier, "9999"));
    }
}
