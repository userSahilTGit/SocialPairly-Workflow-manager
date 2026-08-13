package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.dto.MediaModerationRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUpdateRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileStatusDto;
import com.SocialPairly_Workflow_Manager.entity.AccessRequestStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType; // Your Entity Enum (PHOTO, VIDEO)
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserMedia;
import com.SocialPairly_Workflow_Manager.entity.VideoAccessRequest;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.VideoAccessRequestRepository;
import com.SocialPairly_Workflow_Manager.service.CurrentUserService;
import com.SocialPairly_Workflow_Manager.service.UserMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.HashMap;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserMediaController {

    private final UserMediaService userMediaService;
    private final CurrentUserService currentUserService;
    private final VideoAccessRequestRepository videoAccessRequestRepository;

    // --- User Endpoints ---

    @PostMapping("/media/upload")
    public ResponseEntity<MediaUploadResponseDto> uploadMedia(@RequestParam("file") MultipartFile file) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userMediaService.uploadMedia(currentUser, file));
    }

    // 👈 Streams raw byte[] data directly from MySQL database
    @GetMapping("/media/{id}/stream")
    public ResponseEntity<byte[]> streamMedia(@PathVariable Long id) {
        UserMedia media = userMediaService.getMediaEntityById(id);

        // Dynamic MIME type determination
        String contentType = (media.getMediaType() == MediaType.VIDEO) ? "video/mp4" : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType)) // Fully qualified to avoid collision
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes") // 👈 Enables video seeking/playhead support
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(media.getMediaData().length))
                .body(media.getMediaData());
    }

    @GetMapping("/media/my-uploads")
    public ResponseEntity<List<MediaUploadResponseDto>> getMyUploads() {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userMediaService.getUserMedia(currentUser.getId()));
    }

    // 👈 Privacy query layer (Step 1):
    // Returns media on another user's profile that the logged-in viewer is permitted to see.
    // PUBLIC + APPROVED is always returned; MUTUAL_ONLY is returned only when viewers are mutual.
    // Example: an authenticated user browsing userId=5's profile calls GET /api/users/5/media.
    @GetMapping("/users/{id}/media")
    public ResponseEntity<List<MediaUploadResponseDto>> getUserMediaForViewer(@PathVariable Long id) {
        User viewer = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userMediaService.getVisibleMediaForViewer(id, viewer.getId()));
    }

    // 👈 Photo verification (Step 5): face comparison runs 100% client-side via
    // face-api.js. The frontend calls this endpoint after confirming the match
    // to award the verified badge. No image bytes are sent to the backend.
    @PostMapping("/users/verify-success")
    public ResponseEntity<?> verifySuccess() {
        User currentUser = currentUserService.getCurrentUser();
        currentUser.setVerified(true);
        currentUserService.save(currentUser);
        return ResponseEntity.ok(Map.of(
                "isVerified", true,
                "message", "Identity verified successfully."
        ));
    }

    @DeleteMapping("/media/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser();
        userMediaService.deleteMedia(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/media/{id}")
    public ResponseEntity<MediaUploadResponseDto> updateMedia(
            @PathVariable Long id,
            @RequestBody MediaUpdateRequestDto request) {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userMediaService.updateMedia(currentUser, id, request));
    }

    // --- Admin Endpoints ---

    @GetMapping("/admin/media/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MediaUploadResponseDto>> getAllMedia() {
        return ResponseEntity.ok(userMediaService.getAllMediaForAdmin());
    }

    @GetMapping("/admin/media/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MediaUploadResponseDto>> getPendingMedia() {
        return ResponseEntity.ok(userMediaService.getPendingMediaForAdmin());
    }

    @PutMapping("/admin/media/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MediaUploadResponseDto> moderateMedia(
            @PathVariable Long id,
            @RequestBody MediaModerationRequestDto request) {
        return ResponseEntity.ok(userMediaService.moderateMedia(id, request));
    }

    // 👈 Video access gating: request access to a gated video
    @PostMapping("/users/{userId}/media/{mediaId}/request-access")
    public ResponseEntity<?> requestVideoAccess(@PathVariable Long userId, @PathVariable Long mediaId) {
        User requester = currentUserService.getCurrentUser();
        UserMedia media = userMediaService.getMediaEntityById(mediaId);
        
        // IDOR prevention: verify the media belongs to the target user
        if (!media.getUser().getId().equals(userId)) {
            throw new BadRequestException("Media not found.");
        }
        
        // Only videos can have access restrictions
        if (media.getMediaType() != MediaType.VIDEO) {
            throw new BadRequestException("Access requests are only for videos.");
        }
        
        // Check if request already exists
        boolean alreadyRequested = videoAccessRequestRepository
                .existsByMediaIdAndRequesterIdAndStatus(mediaId, requester.getId(), AccessRequestStatus.PENDING);
        if (alreadyRequested) {
            return ResponseEntity.ok(Map.of("message", "Access request already pending."));
        }
        
        VideoAccessRequest request = VideoAccessRequest.builder()
                .media(media)
                .requester(requester)
                .owner(media.getUser())
                .status(AccessRequestStatus.PENDING)
                .build();
        
        videoAccessRequestRepository.save(request);
        return ResponseEntity.ok(Map.of("message", "Access request sent to user."));
    }

    // 👈 Get pending access requests for the current user's videos
    @GetMapping("/media/access-requests")
    public ResponseEntity<List<Map<String, Object>>> getAccessRequests() {
        User currentUser = currentUserService.getCurrentUser();
        List<VideoAccessRequest> requests = videoAccessRequestRepository.findByOwnerIdAndStatus(
                currentUser.getId(), AccessRequestStatus.PENDING);
        
        List<Map<String, Object>> result = requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("mediaId", req.getMedia().getId());
            map.put("requesterName", req.getRequester().getFirstName() + " " + req.getRequester().getLastName());
            map.put("requesterId", req.getRequester().getId());
            map.put("createdAt", req.getCreatedAt().toString());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    // 👈 Approve or reject an access request
    @PutMapping("/media/access-requests/{requestId}/respond")
    public ResponseEntity<?> respondToAccessRequest(@PathVariable Long requestId, @RequestBody Map<String, String> body) {
        User currentUser = currentUserService.getCurrentUser();
        VideoAccessRequest request = videoAccessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found."));
        
        // Only the owner can respond
        if (!request.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only respond to requests for your own media.");
        }
        
        String action = body.get("action");
        if (!"APPROVED".equalsIgnoreCase(action) && !"REJECTED".equalsIgnoreCase(action)) {
            throw new BadRequestException("Action must be APPROVED or REJECTED.");
        }
        
        request.setStatus(AccessRequestStatus.valueOf(action.toUpperCase()));
        videoAccessRequestRepository.save(request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Access request " + action.toLowerCase() + ".",
                "status", request.getStatus().name()
        ));
    }

    // 👈 Profile readiness check
    @GetMapping("/users/profile-readiness")
    public ResponseEntity<ProfileStatusDto> getProfileReadiness() {
        User currentUser = currentUserService.getCurrentUser();
        return ResponseEntity.ok(userMediaService.evaluateProfileReadiness(currentUser.getId()));
    }
}
