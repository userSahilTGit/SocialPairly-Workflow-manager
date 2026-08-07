package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserMediaTest {

    @Test
    @DisplayName("onCreate sets timestamps and defaults status to APPROVED")
    void onCreate_setsDefaults() {
        UserMedia media = new UserMedia();
        assertNull(media.getCreatedAt());
        assertNull(media.getUpdatedAt());
        assertNull(media.getStatus());
        ReflectionTestUtils.invokeMethod(media, "onCreate");
        assertNotNull(media.getCreatedAt());
        assertNotNull(media.getUpdatedAt());
        assertEquals(MediaStatus.APPROVED, media.getStatus());
    }

    @Test
    @DisplayName("onCreate preserves existing status")
    void onCreate_preservesStatus() {
        UserMedia media = new UserMedia();
        media.setStatus(MediaStatus.PENDING);
        ReflectionTestUtils.invokeMethod(media, "onCreate");
        assertEquals(MediaStatus.PENDING, media.getStatus());
    }

    @Test
    @DisplayName("onUpdate sets updatedAt")
    void onUpdate_setsUpdatedAt() {
        UserMedia media = new UserMedia();
        media.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        ReflectionTestUtils.invokeMethod(media, "onUpdate");
        assertNotNull(media.getUpdatedAt());
        assertTrue(media.getUpdatedAt().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }

    @Test
    @DisplayName("UserMedia - builder works")
    void builderAndGettersSetters() {
        UserMedia media = UserMedia.builder()
                .id(10L)
                .mediaType(MediaType.VIDEO)
                .fileSizeKb(2.5)
                .status(MediaStatus.PENDING)
                .isCover(false)
                .requiresAccessApproval(true)
                .build();
        assertEquals(10L, media.getId());
        assertEquals(MediaType.VIDEO, media.getMediaType());
        assertEquals(2.5, media.getFileSizeKb());
        assertEquals(MediaStatus.PENDING, media.getStatus());
        assertFalse(media.getIsCover());
        assertTrue(media.getRequiresAccessApproval());
    }

    @Test
    @DisplayName("isCover and requiresAccessApproval default to false")
    void booleanDefaults() {
        UserMedia media = new UserMedia();
        assertFalse(media.getIsCover());
        assertFalse(media.getRequiresAccessApproval());
    }

    @Test
    @DisplayName("full constructor is accessible all-args")
    void allArgsConstructor() {
        User user = new User();
        user.setId(1L);
        byte[] data = new byte[]{1, 2, 3};
        LocalDateTime now = LocalDateTime.now();
        UserMedia media = new UserMedia(
                99L, user, data, MediaType.PHOTO, 9.9, MediaStatus.APPROVED,
                null, "cap", 2, "HOBBY", "MUTUAL_ONLY", true, "prompt", false, now, now);
        assertEquals(99L, media.getId());
        assertSame(user, media.getUser());
        assertArrayEquals(data, media.getMediaData());
        assertEquals(MediaType.PHOTO, media.getMediaType());
        assertEquals(9.9, media.getFileSizeKb());
        assertEquals(MediaStatus.APPROVED, media.getStatus());
        assertEquals("cap", media.getCaption());
        assertEquals(2, media.getDisplayOrder());
        assertEquals("HOBBY", media.getMediaCategory());
        assertEquals("MUTUAL_ONLY", media.getPrivacyMode());
        assertTrue(media.getIsCover());
        assertEquals("prompt", media.getPromptText());
        assertFalse(media.getRequiresAccessApproval());
        assertEquals(now, media.getCreatedAt());
        assertEquals(now, media.getUpdatedAt());
    }
}
