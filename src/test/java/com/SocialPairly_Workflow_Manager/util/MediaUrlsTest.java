package com.SocialPairly_Workflow_Manager.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaUrlsTest {

    @Test
    void detectsLegacyDiskPaths() {
        assertTrue(MediaUrls.isLegacyDiskPath("/uploads/a.jpg"));
        assertTrue(MediaUrls.isLegacyDiskPath("http://localhost:9010/uploads/a.jpg"));
        assertFalse(MediaUrls.isLegacyDiskPath("/api/media/1/stream"));
        assertFalse(MediaUrls.isLegacyDiskPath(null));
    }

    @Test
    void usableProfilePhotoUrlDropsLegacyAndBlank() {
        assertNull(MediaUrls.usableProfilePhotoUrl(null));
        assertNull(MediaUrls.usableProfilePhotoUrl("  "));
        assertNull(MediaUrls.usableProfilePhotoUrl("/uploads/old.png"));
        assertEquals("/api/media/9/stream", MediaUrls.usableProfilePhotoUrl("/api/media/9/stream"));
        assertEquals("/photo.png", MediaUrls.usableProfilePhotoUrl("/photo.png"));
    }
}
