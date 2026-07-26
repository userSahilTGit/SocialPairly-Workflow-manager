package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceAdditionalTests {

    @Test
    void shouldRejectMissingOtpWhenVerifying() {
        OtpService service = new OtpService();

        assertThrows(BadRequestException.class, () -> service.verify("missing@example.com", "0000"));
    }

    @Test
    void shouldRejectExpiredOtp() {
        OtpService service = new OtpService();
        String otp = service.generateAndStore("user@example.com");

        Object store = ReflectionTestUtils.getField(service, "store");
        assertNotNull(store);

        Map<?, ?> storeMap = (Map<?, ?>) store;
        Object entry = storeMap.get("user@example.com");
        assertNotNull(entry);

        ReflectionTestUtils.setField(entry, "expiresAt", 0L);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verify("user@example.com", otp));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectAssertVerifiedWhenOtpNotVerified() {
        OtpService service = new OtpService();
        String otp = service.generateAndStore("user@example.com");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.assertVerified("user@example.com", otp));
        assertTrue(ex.getMessage().contains("OTP verification required"));
    }
}
