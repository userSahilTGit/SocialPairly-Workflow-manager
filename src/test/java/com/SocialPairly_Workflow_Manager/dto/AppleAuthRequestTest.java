package com.SocialPairly_Workflow_Manager.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppleAuthRequestTest {

    @Test
    void idTokenRoundTrips() {
        AppleAuthRequest request = new AppleAuthRequest();
        assertNull(request.getIdToken());
        request.setIdToken("apple-id-token");
        assertEquals("apple-id-token", request.getIdToken());
    }
}
