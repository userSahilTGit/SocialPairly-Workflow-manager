package com.SocialPairly_Workflow_Manager.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VideoAccessRequestTest {

    @Test
    @DisplayName("onCreate sets createdAt")
    void onCreate_setsCreatedAt() {
        VideoAccessRequest request = new VideoAccessRequest();
        assertNull(request.getCreatedAt());
        ReflectionTestUtils.invokeMethod(request, "onCreate");
        assertNotNull(request.getCreatedAt());
    }

    @Test
    @DisplayName("default status is PENDING")
    void defaultStatusPending() {
        VideoAccessRequest request = new VideoAccessRequest();
        assertEquals(AccessRequestStatus.PENDING, request.getStatus());
    }

    @Test
    @DisplayName("builder builds all fields")
    void builder_buildsAll() {
        User requester = new User();
        requester.setId(1L);
        User owner = new User();
        owner.setId(2L);
        UserMedia media = new UserMedia();
        media.setId(3L);
        VideoAccessRequest request = VideoAccessRequest.builder()
                .id(10L)
                .media(media)
                .requester(requester)
                .owner(owner)
                .status(AccessRequestStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();
        assertEquals(10L, request.getId());
        assertSame(media, request.getMedia());
        assertSame(requester, request.getRequester());
        assertSame(owner, request.getOwner());
        assertEquals(AccessRequestStatus.APPROVED, request.getStatus());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    @DisplayName("setters work")
    void settersWork() {
        VideoAccessRequest request = new VideoAccessRequest();
        request.setStatus(AccessRequestStatus.REJECTED);
        assertEquals(AccessRequestStatus.REJECTED, request.getStatus());
    }
}
