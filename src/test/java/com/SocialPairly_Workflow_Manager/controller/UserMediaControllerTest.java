package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.MediaModerationRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUpdateRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileStatusDto;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.GlobalExceptionHandler;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.UserMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserMediaControllerTest {

    private MockMvc mockMvc;
    private UserMediaService userMediaService;
    private CurrentUserService currentUserService;
    private VideoAccessRequestRepository videoAccessRequestRepository;
    private UserMediaController controller;

    private User currentUser;
    private User owner;
    private User requester;

    @BeforeEach
    void setUp() {
        userMediaService = mock(UserMediaService.class);
        currentUserService = mock(CurrentUserService.class);
        videoAccessRequestRepository = mock(VideoAccessRequestRepository.class);
        controller = new UserMediaController(userMediaService, currentUserService, videoAccessRequestRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setFirstName("John");
        currentUser.setLastName("Doe");
        currentUser.setEmail("john@example.com");

        owner = new User();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");

        requester = new User();
        requester.setId(2L);
        requester.setFirstName("Jane");
        requester.setLastName("Smith");

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    private MediaUploadResponseDto dto(Long id, MediaType type) {
        return MediaUploadResponseDto.builder()
                .id(id).userId(1L).userName("John Doe").mediaUrl("/api/media/" + id + "/stream")
                .mediaType(type).fileSizeKb(1.0).status(MediaStatus.APPROVED)
                .privacyMode("PUBLIC").isCover(false).requiresAccessApproval(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/media/upload - success returns 200")
    void uploadMedia_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});
        when(userMediaService.uploadMedia(eq(currentUser), any())).thenReturn(dto(1L, MediaType.PHOTO));

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mediaUrl").value("/api/media/1/stream"));
    }

    @Test
    @DisplayName("POST /api/media/upload - service throws BadRequest -> 400")
    void uploadMedia_badRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "v.mp4", "video/mp4", new byte[]{1});
        when(userMediaService.uploadMedia(eq(currentUser), any()))
                .thenThrow(new BadRequestException("Video file size exceeds the 5MB limit."));

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/media/{id}/stream - photo returns image/jpeg with headers")
    void streamMedia_photo() throws Exception {
        UserMedia photo = UserMedia.builder()
                .id(1L).user(currentUser)
                .mediaData(new byte[]{1, 2, 3}).mediaType(MediaType.PHOTO)
                .fileSizeKb(1.0).status(MediaStatus.APPROVED).requiresAccessApproval(false)
                .build();
        when(userMediaService.getMediaEntityById(1L)).thenReturn(photo);

        mockMvc.perform(get("/api/media/1/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "3"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("GET /api/media/{id}/stream - video returns video/mp4")
    void streamMedia_video() throws Exception {
        UserMedia video = UserMedia.builder()
                .id(2L).user(currentUser)
                .mediaData(new byte[]{9, 9}).mediaType(MediaType.VIDEO)
                .fileSizeKb(1.0).status(MediaStatus.APPROVED).requiresAccessApproval(false)
                .build();
        when(userMediaService.getMediaEntityById(2L)).thenReturn(video);

        mockMvc.perform(get("/api/media/2/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "2"))
                .andExpect(content().bytes(new byte[]{9, 9}));
    }

    @Test
    @DisplayName("GET /api/media/{id}/stream - not found -> 404")
    void streamMedia_notFound() throws Exception {
        when(userMediaService.getMediaEntityById(99L))
                .thenThrow(new ResourceNotFoundException("Media not found with id: 99"));
        mockMvc.perform(get("/api/media/99/stream")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/media/my-uploads - returns list")
    void getMyUploads_success() throws Exception {
        when(userMediaService.getUserMedia(1L)).thenReturn(List.of(dto(1L, MediaType.PHOTO)));
        mockMvc.perform(get("/api/media/my-uploads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/users/{id}/media - returns visible media for viewer")
    void getUserMediaForViewer_success() throws Exception {
        when(userMediaService.getVisibleMediaForViewer(5L, 1L)).thenReturn(List.of(dto(1L, MediaType.PHOTO)));
        mockMvc.perform(get("/api/users/5/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(userMediaService).getVisibleMediaForViewer(5L, 1L);
    }

    @Test
    @DisplayName("POST /api/users/verify-success - sets verified and returns map")
    void verifySuccess_setsVerified() throws Exception {
        when(currentUserService.save(currentUser)).thenReturn(currentUser);
        mockMvc.perform(post("/api/users/verify-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));
        verify(currentUserService).save(currentUser);
    }

    @Test
    @DisplayName("DELETE /api/media/{id} - success returns 204")
    void deleteMedia_success() throws Exception {
        doNothing().when(userMediaService).deleteMedia(currentUser, 1L);
        mockMvc.perform(delete("/api/media/1")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/media/{id} - wrong owner -> 400")
    void deleteMedia_wrongOwner() throws Exception {
        doThrow(new BadRequestException("You can only delete your own media."))
                .when(userMediaService).deleteMedia(currentUser, 1L);
        mockMvc.perform(delete("/api/media/1")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/media/{id} - updates media")
    void updateMedia_success() throws Exception {
        when(userMediaService.updateMedia(eq(currentUser), eq(1L), any(MediaUpdateRequestDto.class)))
                .thenReturn(dto(1L, MediaType.PHOTO));
        mockMvc.perform(put("/api/media/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"caption\":\"Hello\",\"requiresAccessApproval\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/media/{id} - not found -> 404")
    void updateMedia_notFound() throws Exception {
        when(userMediaService.updateMedia(eq(currentUser), eq(1L), any(MediaUpdateRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Media not found with id: 1"));
        mockMvc.perform(put("/api/media/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/admin/media/all - returns all media")
    void getAllMedia_admin() throws Exception {
        when(userMediaService.getAllMediaForAdmin()).thenReturn(List.of(dto(1L, MediaType.PHOTO)));
        mockMvc.perform(get("/api/admin/media/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/media/pending - returns pending")
    void getPendingMedia_admin() throws Exception {
        when(userMediaService.getPendingMediaForAdmin()).thenReturn(List.of(dto(5L, MediaType.PHOTO)));
        mockMvc.perform(get("/api/admin/media/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @DisplayName("PUT /api/admin/media/{id}/moderate - moderates media")
    void moderateMedia_admin() throws Exception {
        when(userMediaService.moderateMedia(eq(1L), any(MediaModerationRequestDto.class)))
                .thenReturn(dto(1L, MediaType.PHOTO));
        mockMvc.perform(put("/api/admin/media/1/moderate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/admin/media/{id}/moderate - rejected without reason -> 400")
    void moderateMedia_rejectedNoReason() throws Exception {
        when(userMediaService.moderateMedia(eq(1L), any(MediaModerationRequestDto.class)))
                .thenThrow(new BadRequestException("Rejection reason is required when rejecting content."));
        mockMvc.perform(put("/api/admin/media/1/moderate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest());
    }

    private UserMedia videoMedia(long id) {
        return UserMedia.builder()
                .id(id).user(owner).mediaData(new byte[]{1}).mediaType(MediaType.VIDEO)
                .fileSizeKb(1.0).status(MediaStatus.APPROVED).requiresAccessApproval(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/users/{userId}/media/{mediaId}/request-access - success")
    void requestVideoAccess_success() throws Exception {
        UserMedia media = videoMedia(10L);
        when(userMediaService.getMediaEntityById(10L)).thenReturn(media);
        when(videoAccessRequestRepository.existsByMediaIdAndRequesterIdAndStatus(10L, 1L, AccessRequestStatus.PENDING))
                .thenReturn(false);
        when(videoAccessRequestRepository.save(any(VideoAccessRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/users/1/media/10/request-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access request sent to user."));
        verify(videoAccessRequestRepository).save(any(VideoAccessRequest.class));
    }

    @Test
    @DisplayName("POST request-access - IDOR mismatch throws BadRequest")
    void requestVideoAccess_idorMismatch() throws Exception {
        UserMedia media = videoMedia(10L);
        when(userMediaService.getMediaEntityById(10L)).thenReturn(media);
        mockMvc.perform(post("/api/users/99/media/10/request-access"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST request-access - non-video media throws BadRequest")
    void requestVideoAccess_nonVideo() throws Exception {
        UserMedia photo = UserMedia.builder()
                .id(11L).user(owner).mediaData(new byte[]{1}).mediaType(MediaType.PHOTO)
                .fileSizeKb(1.0).status(MediaStatus.APPROVED).requiresAccessApproval(false)
                .build();
        when(userMediaService.getMediaEntityById(11L)).thenReturn(photo);
        mockMvc.perform(post("/api/users/1/media/11/request-access"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST request-access - already pending returns message")
    void requestVideoAccess_alreadyPending() throws Exception {
        UserMedia media = videoMedia(10L);
        when(userMediaService.getMediaEntityById(10L)).thenReturn(media);
        when(videoAccessRequestRepository.existsByMediaIdAndRequesterIdAndStatus(10L, 1L, AccessRequestStatus.PENDING))
                .thenReturn(true);
        mockMvc.perform(post("/api/users/1/media/10/request-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access request already pending."));
        verify(videoAccessRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET /api/media/access-requests - returns pending requests map")
    void getAccessRequests_success() throws Exception {
        VideoAccessRequest req = VideoAccessRequest.builder()
                .id(5L).requester(requester).owner(currentUser)
                .media(videoMedia(10L)).status(AccessRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(videoAccessRequestRepository.findByOwnerIdAndStatus(1L, AccessRequestStatus.PENDING))
                .thenReturn(List.of(req));

        mockMvc.perform(get("/api/media/access-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].mediaId").value(10))
                .andExpect(jsonPath("$[0].requesterName").value("Jane Smith"))
                .andExpect(jsonPath("$[0].requesterId").value(2));
    }

    @Test
    @DisplayName("GET /api/media/access-requests - empty list")
    void getAccessRequests_empty() throws Exception {
        when(videoAccessRequestRepository.findByOwnerIdAndStatus(1L, AccessRequestStatus.PENDING))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/media/access-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("PUT /api/media/access-requests/{id}/respond - approve")
    void respondToAccessRequest_approve() throws Exception {
        VideoAccessRequest req = VideoAccessRequest.builder()
                .id(5L).requester(requester).owner(currentUser)
                .media(videoMedia(10L)).status(AccessRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(videoAccessRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        when(videoAccessRequestRepository.save(any(VideoAccessRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/media/access-requests/5/respond")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("Access request approved."));
    }

    @Test
    @DisplayName("PUT respond - reject")
    void respondToAccessRequest_reject() throws Exception {
        VideoAccessRequest req = VideoAccessRequest.builder()
                .id(5L).requester(requester).owner(currentUser)
                .media(videoMedia(10L)).status(AccessRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(videoAccessRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        when(videoAccessRequestRepository.save(any(VideoAccessRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/media/access-requests/5/respond")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"rejected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Access request rejected."));
    }

    @Test
    @DisplayName("PUT respond - request not found -> 404")
    void respondToAccessRequest_notFound() throws Exception {
        when(videoAccessRequestRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(put("/api/media/access-requests/99/respond")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT respond - not owner -> 400")
    void respondToAccessRequest_notOwner() throws Exception {
        User otherOwner = new User();
        otherOwner.setId(3L);
        VideoAccessRequest req = VideoAccessRequest.builder()
                .id(5L).requester(requester).owner(otherOwner)
                .media(videoMedia(10L)).status(AccessRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(videoAccessRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        mockMvc.perform(put("/api/media/access-requests/5/respond")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT respond - invalid action -> 400")
    void respondToAccessRequest_invalidAction() throws Exception {
        VideoAccessRequest req = VideoAccessRequest.builder()
                .id(5L).requester(requester).owner(currentUser)
                .media(videoMedia(10L)).status(AccessRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(videoAccessRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        mockMvc.perform(put("/api/media/access-requests/5/respond")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HACK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users/profile-readiness - returns DTO")
    void getProfileReadiness_success() throws Exception {
        ProfileStatusDto readiness = ProfileStatusDto.builder()
                .isProfilePublished(false).isVerified(false).photoCount(2).videoCount(0)
                .hasFullLength(false).hasPrimaryCover(false).meetsMinimumRequirements(false)
                .missingRequirements(List.of("At least 3 approved photos are required (Current: 2)."))
                .build();
        when(userMediaService.evaluateProfileReadiness(1L)).thenReturn(readiness);

        mockMvc.perform(get("/api/users/profile-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoCount").value(2))
                .andExpect(jsonPath("$.meetsMinimumRequirements").value(false));
    }
}