package com.SocialPairly_Workflow_Manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImageModerationServiceTest {

    private ImageModerationService service;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        service = new ImageModerationService("test-key");
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("constructor - blank key disabled")
    void constructor_blankKey_disabled() {
        ImageModerationService svc = new ImageModerationService("");
        assertFalse(svc.isEnabled());
    }

    @Test
    @DisplayName("constructor - null key disabled")
    void constructor_nullKey_disabled() {
        ImageModerationService svc = new ImageModerationService(null);
        assertFalse(svc.isEnabled());
    }

    @Test
    @DisplayName("constructor - key present enabled")
    void constructor_keyPresent_enables() {
        ImageModerationService svc = new ImageModerationService("abc123");
        assertTrue(svc.isEnabled());
    }

    @Test
    @DisplayName("isEnabled - true when key set")
    void isEnabled_true() {
        assertTrue(service.isEnabled());
    }

    @Test
    @DisplayName("isImageSafe - disabled -> true")
    void isImageSafe_disabled_true() {
        ImageModerationService disabled = new ImageModerationService("  ");
        assertTrue(disabled.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - null bytes -> true")
    void isImageSafe_nullBytes_true() {
        assertTrue(service.isImageSafe(null));
    }

    @Test
    @DisplayName("isImageSafe - empty bytes -> true")
    void isImageSafe_emptyBytes_true() {
        assertTrue(service.isImageSafe(new byte[0]));
    }

    @Test
    @DisplayName("isImageSafe - rating e -> true")
    void isImageSafe_ratingE_true() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("rating_letter", "e"));
        assertTrue(service.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - rating E uppercase -> true")
    void isImageSafe_ratingEUppercase_true() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("rating_letter", "E"));
        assertTrue(service.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - rating a -> false")
    void isImageSafe_ratingA_false() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("rating_letter", "a"));
        assertFalse(service.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - null rating -> true fail-open")
    void isImageSafe_nullRating_true() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("other", "value"));
        assertTrue(service.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - null response -> true fail-open")
    void isImageSafe_nullResponse_true() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);
        assertTrue(service.isImageSafe(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("isImageSafe - API throws -> true fail-open")
    void isImageSafe_exception_true() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("api down"));
        assertTrue(service.isImageSafe(new byte[]{1, 2, 3}));
    }
}
