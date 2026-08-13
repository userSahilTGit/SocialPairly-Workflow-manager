package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.MediaModerationRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUpdateRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileStatusDto;
import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserMedia;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMediaServiceTest {

    @Mock
    private UserMediaRepository userMediaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TextModerationService textModerationService;
    @Mock
    private ImageModerationService imageModerationService;

    private UserMediaService service;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        service = new UserMediaService(userMediaRepository, userRepository,
                textModerationService, imageModerationService);
        owner = new User();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john@example.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setFirstName("Jane");
        otherUser.setLastName("Smith");
        otherUser.setEmail("jane@example.com");
    }

    // ==================== uploadMedia ====================

    @Test
    @DisplayName("uploadMedia - null content type throws BadRequest")
    void uploadMedia_nullContentType_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", null, new byte[]{1});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertEquals("Invalid file format.", ex.getMessage());
        verifyNoInteractions(userMediaRepository);
    }

    @Test
    @DisplayName("uploadMedia - unsupported file type throws BadRequest")
    void uploadMedia_unsupportedType_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertEquals("Only Image and Video files are supported.", ex.getMessage());
        // Repository is never reached because the type check throws first.
        verifyNoInteractions(userMediaRepository);
    }

    @Test
    @DisplayName("uploadMedia - video over 5MB throws BadRequest")
    void uploadMedia_videoTooLarge_throwsBadRequest() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "v.mp4", "video/mp4", big);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertEquals("Video file size exceeds the 5MB limit.", ex.getMessage());
        // Repository is never reached because the size check throws first.
        verifyNoInteractions(userMediaRepository);
    }

    @Test
    @DisplayName("uploadMedia - photo limit (8) reached throws BadRequest")
    void uploadMedia_photoLimit_throwsBadRequest() {
        List<UserMedia> existing = photos(8);
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});
        when(userMediaRepository.findByUserId(1L)).thenReturn(existing);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertEquals("You can only upload up to 8 photos.", ex.getMessage());
    }

    @Test
    @DisplayName("uploadMedia - video limit (3) reached throws BadRequest")
    void uploadMedia_videoLimit_throwsBadRequest() {
        List<UserMedia> existing = videos(3);
        MockMultipartFile file = new MockMultipartFile("file", "v.mp4", "video/mp4", new byte[]{1});
        when(userMediaRepository.findByUserId(1L)).thenReturn(existing);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertEquals("You can only upload up to 3 videos.", ex.getMessage());
    }

    @Test
    @DisplayName("uploadMedia - photo, moderation disabled, becomes APPROVED and cover")
    void uploadMedia_photo_moderationDisabled_approvedAndCover() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());
        when(imageModerationService.isEnabled()).thenReturn(false);

        UserMedia saved = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        saved.setIsCover(true);
        saved.setMediaCategory("FULL_LENGTH");
        saved.setRequiresAccessApproval(false);
        when(userMediaRepository.save(any(UserMedia.class))).thenReturn(saved);

        MediaUploadResponseDto dto = service.uploadMedia(owner, file);

        assertNotNull(dto);
        assertEquals(MediaType.PHOTO, dto.getMediaType());
        assertEquals(MediaStatus.APPROVED, dto.getStatus());
        assertTrue(dto.getIsCover());
        assertEquals("FULL_LENGTH", dto.getMediaCategory());
        assertFalse(dto.getRequiresAccessApproval());
        assertEquals("/api/media/1/stream", dto.getMediaUrl());
        assertEquals("John Doe", dto.getUserName());
        assertEquals(1L, dto.getUserId());
    }

    @Test
    @DisplayName("uploadMedia - video, moderation disabled, APPROVED not cover")
    void uploadMedia_video_moderationDisabled_approvedNotCover() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "v.mp4", "video/mp4", new byte[]{1, 2, 3, 4});
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());
        when(imageModerationService.isEnabled()).thenReturn(false);

        UserMedia saved = media(2L, MediaType.VIDEO, MediaStatus.APPROVED);
        saved.setIsCover(false);
        saved.setMediaCategory(null);
        when(userMediaRepository.save(any(UserMedia.class))).thenReturn(saved);

        MediaUploadResponseDto dto = service.uploadMedia(owner, file);
        assertEquals(MediaType.VIDEO, dto.getMediaType());
        assertFalse(dto.getIsCover());
        assertNull(dto.getMediaCategory());
    }

    @Test
    @DisplayName("uploadMedia - moderation enabled, photo safe -> APPROVED")
    void uploadMedia_photo_moderationEnabled_safe_approved() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());
        when(imageModerationService.isEnabled()).thenReturn(true);
        when(imageModerationService.isImageSafe(any())).thenReturn(true);
        // Return the argument passed to save so the second (re-save after moderation) works.
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaUploadResponseDto dto = service.uploadMedia(owner, file);
        assertEquals(MediaStatus.APPROVED, dto.getStatus());
        verify(userMediaRepository, times(2)).save(any(UserMedia.class));
    }

    @Test
    @DisplayName("uploadMedia - moderation enabled, photo unsafe -> REJECTED with reason")
    void uploadMedia_photo_moderationEnabled_unsafe_rejected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());
        when(imageModerationService.isEnabled()).thenReturn(true);
        when(imageModerationService.isImageSafe(any())).thenReturn(false);
        // Return the argument passed to save so the second (re-save after moderation) works.
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaUploadResponseDto dto = service.uploadMedia(owner, file);
        assertEquals(MediaStatus.REJECTED, dto.getStatus());
        assertEquals("Image flagged by content moderation.", dto.getRejectionReason());
        verify(userMediaRepository, times(2)).save(any(UserMedia.class));
    }

    @Test
    @DisplayName("uploadMedia - IO exception -> BadRequest")
    void uploadMedia_ioException_throwsBadRequest() {
        MultipartFile file = new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return "p.jpg"; }
            @Override public String getContentType() { return "image/jpeg"; }
            @Override public boolean isEmpty() { return false; }
            @Override public long getSize() { return 3; }
            @Override public byte[] getBytes() throws IOException { throw new IOException("boom"); }
            @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
            @Override public void transferTo(java.io.File dest) { }
        };
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadMedia(owner, file));
        assertTrue(ex.getMessage().contains("Failed to read file bytes"));
    }

    // ==================== getMediaEntityById ====================

    @Test
    @DisplayName("getMediaEntityById - found")
    void getMediaEntityById_found() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        assertEquals(m, service.getMediaEntityById(1L));
    }

    @Test
    @DisplayName("getMediaEntityById - not found throws")
    void getMediaEntityById_notFound_throws() {
        when(userMediaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMediaEntityById(99L));
    }

    // ==================== getUserMedia ====================

    @Test
    @DisplayName("getUserMedia - returns mapped DTOs")
    void getUserMedia_returnsMapped() {
        List<UserMedia> list = List.of(
                media(1L, MediaType.PHOTO, MediaStatus.APPROVED),
                media(2L, MediaType.VIDEO, MediaStatus.APPROVED));
        when(userMediaRepository.findByUserIdOrderByDisplayOrderAsc(1L)).thenReturn(list);
        List<MediaUploadResponseDto> result = service.getUserMedia(1L);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(MediaType.VIDEO, result.get(1).getMediaType());
    }

    // ==================== getAllMediaForAdmin / getPendingMediaForAdmin ====================

    @Test
    @DisplayName("getAllMediaForAdmin - returns all mapped")
    void getAllMediaForAdmin() {
        when(userMediaRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(media(1L, MediaType.PHOTO, MediaStatus.APPROVED)));
        List<MediaUploadResponseDto> result = service.getAllMediaForAdmin();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("getPendingMediaForAdmin - returns pending mapped")
    void getPendingMediaForAdmin() {
        when(userMediaRepository.findByStatus(MediaStatus.PENDING))
                .thenReturn(List.of(media(5L, MediaType.PHOTO, MediaStatus.PENDING)));
        List<MediaUploadResponseDto> result = service.getPendingMediaForAdmin();
        assertEquals(1, result.size());
        assertEquals(MediaStatus.PENDING, result.get(0).getStatus());
    }

    // ==================== getVisibleMediaForViewer ====================

    @Test
    @DisplayName("getVisibleMediaForViewer - verified viewer sees VERIFIED_ONLY")
    void getVisibleMediaForViewer_verifiedViewer() {
        User viewer = new User();
        viewer.setId(2L);
        viewer.setVerified(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewer));
        when(userMediaRepository.findVisibleMediaForViewer(1L, false, true, false))
                .thenReturn(List.of(media(1L, MediaType.PHOTO, MediaStatus.APPROVED)));
        List<MediaUploadResponseDto> result = service.getVisibleMediaForViewer(1L, 2L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getVisibleMediaForViewer - unverified viewer does not see VERIFIED_ONLY")
    void getVisibleMediaForViewer_unverifiedViewer() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userMediaRepository.findVisibleMediaForViewer(1L, false, false, false))
                .thenReturn(List.of());
        assertEquals(0, service.getVisibleMediaForViewer(1L, 2L).size());
    }

    @Test
    @DisplayName("getVisibleMediaForViewer - viewer not found -> isVerified false")
    void getVisibleMediaForViewer_viewerNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(userMediaRepository.findVisibleMediaForViewer(1L, false, false, false))
                .thenReturn(List.of());
        assertEquals(0, service.getVisibleMediaForViewer(1L, 2L).size());
    }

    // ==================== isMutual ====================

    @Test
    @DisplayName("isMutual - null target returns false")
    void isMutual_nullTarget() {
        assertFalse(service.isMutual(null, 2L));
    }

    @Test
    @DisplayName("isMutual - null viewer returns false")
    void isMutual_nullViewer() {
        assertFalse(service.isMutual(1L, null));
    }

    @Test
    @DisplayName("isMutual - same user returns false")
    void isMutual_self() {
        assertFalse(service.isMutual(1L, 1L));
    }

    @Test
    @DisplayName("isMutual - different users currently returns false (no friendship model)")
    void isMutual_different() {
        assertFalse(service.isMutual(1L, 2L));
    }

    // ==================== evaluateProfileReadiness ====================

    @Test
    @DisplayName("evaluateProfileReadiness - meets requirements, verified, has full-length & cover")
    void evaluateProfileReadiness_meetsAll() {
        List<UserMedia> approved = new ArrayList<>();
        approved.add(photo(101L, "FULL_LENGTH", true, MediaStatus.APPROVED));
        approved.add(photo(102L, "LIFESTYLE", false, MediaStatus.APPROVED));
        approved.add(photo(103L, "HOBBY", false, MediaStatus.APPROVED));
        approved.add(video(104L));
        when(userMediaRepository.findByUserIdAndStatus(1L, MediaStatus.APPROVED)).thenReturn(approved);
        owner.setVerified(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ProfileStatusDto dto = service.evaluateProfileReadiness(1L);
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
    @DisplayName("evaluateProfileReadiness - below 3 photos, not verified, no cover")
    void evaluateProfileReadiness_belowMinimum() {
        List<UserMedia> approved = new ArrayList<>();
        approved.add(photo(101L, "LIFESTYLE", false, MediaStatus.APPROVED));
        approved.add(photo(102L, "HOBBY", false, MediaStatus.APPROVED));
        when(userMediaRepository.findByUserIdAndStatus(1L, MediaStatus.APPROVED)).thenReturn(approved);
        owner.setVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ProfileStatusDto dto = service.evaluateProfileReadiness(1L);
        assertFalse(dto.isProfilePublished());
        assertFalse(dto.isVerified());
        assertEquals(2, dto.getPhotoCount());
        assertFalse(dto.isHasFullLength());
        assertFalse(dto.isHasPrimaryCover());
        assertFalse(dto.isMeetsMinimumRequirements());
        assertTrue(dto.getMissingRequirements().stream()
                .anyMatch(r -> r.contains("At least 3 approved photos")));
    }

    @Test
    @DisplayName("evaluateProfileReadiness - user not found -> verified false")
    void evaluateProfileReadiness_userNotFound() {
        when(userMediaRepository.findByUserIdAndStatus(1L, MediaStatus.APPROVED)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ProfileStatusDto dto = service.evaluateProfileReadiness(1L);
        assertFalse(dto.isVerified());
        assertFalse(dto.isMeetsMinimumRequirements());
    }

    // ==================== deleteMedia ====================

    @Test
    @DisplayName("deleteMedia - not found throws")
    void deleteMedia_notFound() {
        when(userMediaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteMedia(owner, 1L));
    }

    @Test
    @DisplayName("deleteMedia - wrong owner throws")
    void deleteMedia_wrongOwner() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        m.setUser(otherUser);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(BadRequestException.class, () -> service.deleteMedia(owner, 1L));
    }

    @Test
    @DisplayName("deleteMedia - success deletes")
    void deleteMedia_success() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        m.setUser(owner);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        service.deleteMedia(owner, 1L);
        verify(userMediaRepository).delete(m);
    }

    // ==================== updateMedia ====================

    @Test
    @DisplayName("updateMedia - not found throws")
    void updateMedia_notFound() {
        when(userMediaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateMedia(owner, 1L, new MediaUpdateRequestDto()));
    }

    @Test
    @DisplayName("updateMedia - wrong owner throws")
    void updateMedia_wrongOwner() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        m.setUser(otherUser);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(BadRequestException.class,
                () -> service.updateMedia(owner, 1L, new MediaUpdateRequestDto()));
    }

    @Test
    @DisplayName("updateMedia - all fields set -> saved & mapped")
    void updateMedia_allFields() {
        UserMedia m = media(1L, MediaType.VIDEO, MediaStatus.APPROVED);
        m.setUser(owner);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaUpdateRequestDto req = new MediaUpdateRequestDto();
        req.setCaption("New cap");
        req.setDisplayOrder(3);
        req.setMediaCategory("LIFESTYLE");
        req.setPrivacyMode("MUTUAL_ONLY");
        req.setIsCover(true);
        req.setPromptText("Prompt?");
        req.setRequiresAccessApproval(true);

        MediaUploadResponseDto dto = service.updateMedia(owner, 1L, req);
        assertEquals("New cap", dto.getCaption());
        assertEquals(3, dto.getDisplayOrder());
        assertEquals("LIFESTYLE", dto.getMediaCategory());
        assertEquals("MUTUAL_ONLY", dto.getPrivacyMode());
        assertTrue(dto.getIsCover());
        assertEquals("Prompt?", dto.getPromptText());
        assertTrue(dto.getRequiresAccessApproval());
    }

    @Test
    @DisplayName("updateMedia - only promptText and accessApproval set")
    void updateMedia_partialFields() {
        UserMedia m = media(1L, MediaType.VIDEO, MediaStatus.APPROVED);
        m.setUser(owner);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaUpdateRequestDto req = new MediaUpdateRequestDto();
        req.setPromptText("Hello?");
        req.setRequiresAccessApproval(true);

        MediaUploadResponseDto dto = service.updateMedia(owner, 1L, req);
        assertEquals("Hello?", dto.getPromptText());
        assertTrue(dto.getRequiresAccessApproval());
        assertNull(dto.getCaption());
        assertNull(dto.getDisplayOrder());
    }

    @Test
    @DisplayName("updateMedia - isCover set false")
    void updateMedia_coverFalse() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.APPROVED);
        m.setUser(owner);
        m.setIsCover(true);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaUpdateRequestDto req = new MediaUpdateRequestDto();
        req.setIsCover(false);
        MediaUploadResponseDto dto = service.updateMedia(owner, 1L, req);
        assertFalse(dto.getIsCover());
    }

    // ==================== moderateMedia ====================

    @Test
    @DisplayName("moderateMedia - not found throws")
    void moderateMedia_notFound() {
        when(userMediaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.moderateMedia(1L, new MediaModerationRequestDto()));
    }

    @Test
    @DisplayName("moderateMedia - REJECTED without reason throws")
    void moderateMedia_rejectedNoReason() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.PENDING);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        MediaModerationRequestDto req = new MediaModerationRequestDto();
        req.setStatus(MediaStatus.REJECTED);
        req.setRejectionReason(null);
        assertThrows(BadRequestException.class, () -> service.moderateMedia(1L, req));
    }

    @Test
    @DisplayName("moderateMedia - REJECTED with blank reason throws")
    void moderateMedia_rejectedBlankReason() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.PENDING);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        MediaModerationRequestDto req = new MediaModerationRequestDto();
        req.setStatus(MediaStatus.REJECTED);
        req.setRejectionReason("   ");
        assertThrows(BadRequestException.class, () -> service.moderateMedia(1L, req));
    }

    @Test
    @DisplayName("moderateMedia - REJECTED with reason -> saved mapped")
    void moderateMedia_rejectedWithReason() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.PENDING);
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaModerationRequestDto req = new MediaModerationRequestDto();
        req.setStatus(MediaStatus.REJECTED);
        req.setRejectionReason("Inappropriate");
        MediaUploadResponseDto dto = service.moderateMedia(1L, req);
        assertEquals(MediaStatus.REJECTED, dto.getStatus());
        assertEquals("Inappropriate", dto.getRejectionReason());
    }

    @Test
    @DisplayName("moderateMedia - APPROVED clears rejection reason")
    void moderateMedia_approvedClearsReason() {
        UserMedia m = media(1L, MediaType.PHOTO, MediaStatus.REJECTED);
        m.setRejectionReason("Old reason");
        when(userMediaRepository.findById(1L)).thenReturn(Optional.of(m));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaModerationRequestDto req = new MediaModerationRequestDto();
        req.setStatus(MediaStatus.APPROVED);
        MediaUploadResponseDto dto = service.moderateMedia(1L, req);
        assertEquals(MediaStatus.APPROVED, dto.getStatus());
        assertNull(dto.getRejectionReason());
    }

    @Test
    @DisplayName("uploadProfilePhoto - rejects non-image")
    void uploadProfilePhoto_rejectsNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.uploadProfilePhoto(owner, file));
        assertEquals("Only image files are allowed for the profile photo.", ex.getMessage());
        verifyNoInteractions(userMediaRepository);
    }

    @Test
    @DisplayName("uploadProfilePhoto - stores BLOB and returns stream URL")
    void uploadProfilePhoto_createsBlobStreamUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{9, 8, 7});
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> {
            UserMedia saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        MediaUploadResponseDto dto = service.uploadProfilePhoto(owner, file);
        assertEquals("/api/media/42/stream", dto.getMediaUrl());
        assertEquals("PRIMARY", dto.getMediaCategory());
        assertTrue(dto.getIsCover());
        assertEquals(MediaStatus.APPROVED, dto.getStatus());
    }

    @Test
    @DisplayName("uploadProfilePhoto - replaces existing PRIMARY photo bytes")
    void uploadProfilePhoto_replacesExistingPrimary() {
        UserMedia existing = photo(7L, "PRIMARY", true, MediaStatus.APPROVED);
        when(userMediaRepository.findByUserId(1L)).thenReturn(List.of(existing));
        when(userMediaRepository.save(any(UserMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[]{3, 4, 5});
        MediaUploadResponseDto dto = service.uploadProfilePhoto(owner, file);
        assertEquals("/api/media/7/stream", dto.getMediaUrl());
        assertArrayEquals(new byte[]{3, 4, 5}, existing.getMediaData());
    }

    // ==================== helpers ====================

    private UserMedia media(Long id, MediaType type, MediaStatus status) {
        UserMedia m = UserMedia.builder()
                .id(id)
                .user(owner)
                .mediaData(new byte[]{1})
                .mediaType(type)
                .fileSizeKb(1.0)
                .status(status)
                .isCover(false)
                .privacyMode("PUBLIC")
                .requiresAccessApproval(false)
                .createdAt(LocalDateTime.now())
                .build();
        return m;
    }

    private UserMedia photo(Long id, String category, boolean cover, MediaStatus status) {
        UserMedia m = media(id, MediaType.PHOTO, status);
        m.setMediaCategory(category);
        m.setIsCover(cover);
        return m;
    }

    private UserMedia video(Long id) {
        UserMedia m = media(id, MediaType.VIDEO, MediaStatus.APPROVED);
        m.setMediaCategory("VIDEO");
        return m;
    }

    private List<UserMedia> photos(int count) {
        List<UserMedia> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserMedia m = media((long) i, MediaType.PHOTO, MediaStatus.APPROVED);
            m.setIsCover(i == 0);
            list.add(m);
        }
        return list;
    }

    private List<UserMedia> videos(int count) {
        List<UserMedia> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(media((long) i, MediaType.VIDEO, MediaStatus.APPROVED));
        }
        return list;
    }
}