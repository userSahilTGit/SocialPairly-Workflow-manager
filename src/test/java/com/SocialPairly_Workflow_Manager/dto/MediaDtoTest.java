package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MediaDtoTest {

    @Test
    @DisplayName("MediaUploadResponseDto - builder sets all fields")
    void mediaUploadResponseDto_builder() {
        LocalDateTime now = LocalDateTime.now();
        MediaUploadResponseDto dto = MediaUploadResponseDto.builder()
                .id(1L).userId(5L).userName("John Doe").mediaUrl("/api/media/1/stream")
                .mediaType(MediaType.VIDEO).fileSizeKb(2.5).status(MediaStatus.PENDING)
                .rejectionReason("none").caption("Hello").displayOrder(3)
                .mediaCategory("PRIMARY").privacyMode("MUTUAL_ONLY").isCover(true)
                .promptText("Introduce yourself").requiresAccessApproval(true).createdAt(now)
                .build();
        assertEquals(1L, dto.getId());
        assertEquals(5L, dto.getUserId());
        assertEquals("John Doe", dto.getUserName());
        assertEquals("/api/media/1/stream", dto.getMediaUrl());
        assertEquals(MediaType.VIDEO, dto.getMediaType());
        assertEquals(2.5, dto.getFileSizeKb());
        assertEquals(MediaStatus.PENDING, dto.getStatus());
        assertEquals("none", dto.getRejectionReason());
        assertEquals("Hello", dto.getCaption());
        assertEquals(3, dto.getDisplayOrder());
        assertEquals("PRIMARY", dto.getMediaCategory());
        assertEquals("MUTUAL_ONLY", dto.getPrivacyMode());
        assertTrue(dto.getIsCover());
        assertEquals("Introduce yourself", dto.getPromptText());
        assertTrue(dto.getRequiresAccessApproval());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    @DisplayName("MediaUploadResponseDto - no-arg constructor and setters")
    void mediaUploadResponseDto_setters() {
        MediaUploadResponseDto dto = MediaUploadResponseDto.builder().id(2L).userId(9L).status(MediaStatus.APPROVED).build();
        dto.setId(2L);
        dto.setUserId(9L);
        dto.setStatus(MediaStatus.APPROVED);
        assertEquals(2L, dto.getId());
        assertEquals(9L, dto.getUserId());
        assertEquals(MediaStatus.APPROVED, dto.getStatus());
    }

    @Test
    @DisplayName("MediaUpdateRequestDto - setters")
    void mediaUpdateRequestDto_setters() {
        MediaUpdateRequestDto dto = new MediaUpdateRequestDto();
        dto.setCaption("Cap");
        dto.setDisplayOrder(1);
        dto.setMediaCategory("LIFESTYLE");
        dto.setPrivacyMode("PUBLIC");
        dto.setIsCover(false);
        dto.setPromptText("Prompt");
        dto.setRequiresAccessApproval(true);
        assertEquals("Cap", dto.getCaption());
        assertEquals(1, dto.getDisplayOrder());
        assertEquals("LIFESTYLE", dto.getMediaCategory());
        assertEquals("PUBLIC", dto.getPrivacyMode());
        assertFalse(dto.getIsCover());
        assertEquals("Prompt", dto.getPromptText());
        assertTrue(dto.getRequiresAccessApproval());
    }

    @Test
    @DisplayName("MediaUpdateRequestDto - defaults null")
    void mediaUpdateRequestDto_defaults() {
        MediaUpdateRequestDto dto = new MediaUpdateRequestDto();
        assertNull(dto.getCaption());
        assertNull(dto.getDisplayOrder());
        assertNull(dto.getMediaCategory());
        assertNull(dto.getPrivacyMode());
        assertNull(dto.getIsCover());
        assertNull(dto.getPromptText());
        assertNull(dto.getRequiresAccessApproval());
    }

    @Test
    @DisplayName("MediaModerationRequestDto - setters")
    void mediaModerationRequestDto_setters() {
        MediaModerationRequestDto dto = new MediaModerationRequestDto();
        dto.setStatus(MediaStatus.REJECTED);
        dto.setRejectionReason("Inappropriate");
        assertEquals(MediaStatus.REJECTED, dto.getStatus());
        assertEquals("Inappropriate", dto.getRejectionReason());
    }

    @Test
    @DisplayName("MediaModerationRequestDto - defaults null")
    void mediaModerationRequestDto_defaults() {
        MediaModerationRequestDto dto = new MediaModerationRequestDto();
        assertNull(dto.getStatus());
        assertNull(dto.getRejectionReason());
    }

    @Test
    @DisplayName("ProfileStatusDto - builder all fields")
    void profileStatusDto_builder() {
        ProfileStatusDto dto = ProfileStatusDto.builder()
                .isProfilePublished(true).isVerified(true).photoCount(3).videoCount(1)
                .hasFullLength(true).hasPrimaryCover(true).meetsMinimumRequirements(true)
                .missingRequirements(List.of())
                .build();
        assertTrue(dto.isProfilePublished());
        assertTrue(dto.isVerified());
        assertEquals(3, dto.getPhotoCount());
        assertEquals(1, dto.getVideoCount());
        assertTrue(dto.isHasFullLength());
        assertTrue(dto.isHasPrimaryCover());
        assertTrue(dto.isMeetsMinimumRequirements());
        assertTrue(dto.getMissingRequirements().isEmpty());
    }

    @Test
    @DisplayName("ProfileStatusDto - builder with missing requirements")
    void profileStatusDto_missingRequirements() {
        ProfileStatusDto dto = ProfileStatusDto.builder()
                .isProfilePublished(false).isVerified(false).photoCount(2).videoCount(0)
                .hasFullLength(false).hasPrimaryCover(false).meetsMinimumRequirements(false)
                .missingRequirements(List.of("At least 3 approved photos are required (Current: 2)."))
                .build();
        assertFalse(dto.isProfilePublished());
        assertFalse(dto.isVerified());
        assertEquals(2, dto.getPhotoCount());
        assertEquals(0, dto.getVideoCount());
        assertFalse(dto.isHasFullLength());
        assertFalse(dto.isHasPrimaryCover());
        assertFalse(dto.isMeetsMinimumRequirements());
        assertEquals(1, dto.getMissingRequirements().size());
    }
}
