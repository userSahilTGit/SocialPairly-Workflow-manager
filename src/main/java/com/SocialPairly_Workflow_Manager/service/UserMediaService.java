package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.dto.MediaModerationRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUpdateRequestDto;
import com.SocialPairly_Workflow_Manager.dto.MediaUploadResponseDto;
import com.SocialPairly_Workflow_Manager.dto.ProfileStatusDto;
import com.SocialPairly_Workflow_Manager.entity.*;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.UserMediaRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMediaService {

    private final UserMediaRepository userMediaRepository;
    private final UserRepository userRepository;
    private final TextModerationService textModerationService;
    private final ImageModerationService imageModerationService;
    private static final long MAX_VIDEO_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_PHOTOS_PER_USER = 8;
    private static final int MAX_VIDEOS_PER_USER = 3;

    public MediaUploadResponseDto uploadMedia(User user, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("Invalid file format.");
        }

        MediaType mediaType;
        if (contentType.startsWith("image/")) {
            mediaType = MediaType.PHOTO;
        } else if (contentType.startsWith("video/")) {
            mediaType = MediaType.VIDEO;
            if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
                throw new BadRequestException("Video file size exceeds the 5MB limit.");
            }
        } else {
            throw new BadRequestException("Only Image and Video files are supported.");
        }

        // 👈 Enforce per-user counts: max 8 photos and max 3 videos
        List<UserMedia> existing = userMediaRepository.findByUserId(user.getId());
        long photoCount = existing.stream().filter(m -> m.getMediaType() == MediaType.PHOTO).count();
        long videoCount = existing.stream().filter(m -> m.getMediaType() == MediaType.VIDEO).count();

        if (mediaType == MediaType.PHOTO && photoCount >= MAX_PHOTOS_PER_USER) {
            throw new BadRequestException("You can only upload up to " + MAX_PHOTOS_PER_USER + " photos.");
        }
        if (mediaType == MediaType.VIDEO && videoCount >= MAX_VIDEOS_PER_USER) {
            throw new BadRequestException("You can only upload up to " + MAX_VIDEOS_PER_USER + " videos.");
        }

        double fileSizeKb = file.getSize() / 1024.0;

        try {
            byte[] fileBytes = file.getBytes();

            // 👈 Moderation (Step 3) — zero-cost hybrid approach:
            // 1. Text/handle filter: pure Java regex (always active, $0)
            // 2. NSFW check: ModerateContent API (free, no credit card; skip if no key)
            // Photos start as PENDING, then are auto-approved/rejected.
            boolean moderationEnabled = imageModerationService.isEnabled();
            MediaStatus initialStatus = (moderationEnabled && mediaType == MediaType.PHOTO)
                    ? MediaStatus.PENDING
                    : MediaStatus.APPROVED;

            UserMedia userMedia = UserMedia.builder()
                    .user(user)
                    .mediaData(fileBytes) // 👈 Directly save file bytes into MySQL LONGBLOB
                    .mediaType(mediaType)
                    .fileSizeKb(fileSizeKb)
                    .status(initialStatus)
                    .isCover(photoCount == 0 && mediaType == MediaType.PHOTO) // 👈 First photo becomes the cover
                    .mediaCategory(mediaType == MediaType.PHOTO ? "FULL_LENGTH" : null) // 👈 Auto-assign photos to FULL_LENGTH category
                    .privacyMode("PUBLIC") // Default visibility
                    // 👈 CRITICAL: Lombok @Builder ignores field initializers, so
                    // requiresAccessApproval would be null here. The DB column is
                    // NOT NULL, causing "Column 'requires_access_approval' cannot be null".
                    // Explicitly set it to false on every upload.
                    .requiresAccessApproval(false)
                    .build();

            UserMedia saved = userMediaRepository.save(userMedia);

            // 👈 Run moderation checks for photos when image moderation is enabled
            if (moderationEnabled && mediaType == MediaType.PHOTO) {
                boolean nsfwReject = !imageModerationService.isImageSafe(fileBytes);
                // Note: text detection on images would require OCR; the regex filter
                // is applied to captions/bios via TextModerationService elsewhere.
                boolean reject = nsfwReject;
                saved.setStatus(reject ? MediaStatus.REJECTED : MediaStatus.APPROVED);
                if (reject) {
                    saved.setRejectionReason("Image flagged by content moderation.");
                }
                saved = userMediaRepository.save(saved);
            }

            return mapToDto(saved);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file bytes: " + e.getMessage());
        }
    }

    /**
     * Profile avatar: store image bytes in MySQL (LONGBLOB) and return a stream URL.
     * Replaces an existing PRIMARY/cover photo so this does not consume a gallery slot.
     */
    public MediaUploadResponseDto uploadProfilePhoto(User user, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed for the profile photo.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            double fileSizeKb = file.getSize() / 1024.0;

            List<UserMedia> existing = userMediaRepository.findByUserId(user.getId());
            UserMedia avatar = existing.stream()
                    .filter(m -> m.getMediaType() == MediaType.PHOTO)
                    .filter(m -> Boolean.TRUE.equals(m.getIsCover()) || "PRIMARY".equalsIgnoreCase(m.getMediaCategory()))
                    .findFirst()
                    .orElse(null);

            if (avatar != null) {
                avatar.setMediaData(fileBytes);
                avatar.setFileSizeKb(fileSizeKb);
                avatar.setStatus(MediaStatus.APPROVED);
                avatar.setRejectionReason(null);
                avatar.setIsCover(true);
                avatar.setMediaCategory("PRIMARY");
                avatar.setPrivacyMode(avatar.getPrivacyMode() == null ? "PUBLIC" : avatar.getPrivacyMode());
                avatar.setRequiresAccessApproval(false);
                return mapToDto(userMediaRepository.save(avatar));
            }

            UserMedia created = UserMedia.builder()
                    .user(user)
                    .mediaData(fileBytes)
                    .mediaType(MediaType.PHOTO)
                    .fileSizeKb(fileSizeKb)
                    .status(MediaStatus.APPROVED)
                    .isCover(true)
                    .mediaCategory("PRIMARY")
                    .privacyMode("PUBLIC")
                    .requiresAccessApproval(false)
                    .build();
            return mapToDto(userMediaRepository.save(created));
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file bytes: " + e.getMessage());
        }
    }

    public UserMedia getMediaEntityById(Long mediaId) {
        return userMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
    }

    public List<MediaUploadResponseDto> getUserMedia(Long userId) {
        return userMediaRepository.findByUserIdOrderByDisplayOrderAsc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // 👈 Privacy query layer (Step 1) - Expanded:
    // Returns only media that `viewerId` is allowed to see on `targetUserId`'s profile.
    // Supports PUBLIC, MUTUAL_ONLY, VERIFIED_ONLY, and EVENT_ONLY privacy modes.
    public List<MediaUploadResponseDto> getVisibleMediaForViewer(Long targetUserId, Long viewerId) {
        return getVisibleMediaForViewer(targetUserId, viewerId, false);
    }

    public List<MediaUploadResponseDto> getVisibleMediaForEventParticipant(Long targetUserId, Long viewerId) {
        return getVisibleMediaForViewer(targetUserId, viewerId, true);
    }

    public List<MediaUploadResponseDto> getVisibleMediaForViewer(Long targetUserId, Long viewerId, boolean isEventActive) {
        boolean isMutual = isMutual(targetUserId, viewerId);
        User viewer = userRepository.findById(viewerId).orElse(null);
        boolean isVerified = viewer != null && viewer.isVerified();
        return userMediaRepository.findVisibleMediaForViewer(targetUserId, isMutual, isVerified, isEventActive).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // 👈 Seam for future friendship/mutual-friend logic.
    // Currently returns false because no friendship model exists yet.
    protected boolean isMutual(Long targetUserId, Long viewerId) {
        if (targetUserId == null || viewerId == null || targetUserId.equals(viewerId)) {
            return false;
        }
        // TODO: Implement friendship/mutual-friend determination here in the future.
        return false;
    }

    // 👈 Profile readiness evaluation - enforces minimum requirements before publishing
    public ProfileStatusDto evaluateProfileReadiness(Long userId) {
        List<UserMedia> approvedMedia = userMediaRepository.findByUserIdAndStatus(userId, MediaStatus.APPROVED);
        
        long photoCount = approvedMedia.stream().filter(m -> m.getMediaType() == MediaType.PHOTO).count();
        long videoCount = approvedMedia.stream().filter(m -> m.getMediaType() == MediaType.VIDEO).count();
        boolean hasFullLength = approvedMedia.stream().anyMatch(m -> "FULL_LENGTH".equalsIgnoreCase(m.getMediaCategory()));
        boolean hasPrimary = approvedMedia.stream().anyMatch(m -> Boolean.TRUE.equals(m.getIsCover()));

        List<String> missingRequirements = new ArrayList<>();
        if (photoCount < 3) {
            missingRequirements.add("At least 3 approved photos are required (Current: " + photoCount + ").");
        }

        boolean meetsMinimumRequirements = missingRequirements.isEmpty();
        // Auto-publish when requirements are met (can be triggered after save)
        boolean isProfilePublished = meetsMinimumRequirements;

        return ProfileStatusDto.builder()
                .isProfilePublished(isProfilePublished)
                .isVerified(userRepository.findById(userId).map(User::isVerified).orElse(false))
                .photoCount((int) photoCount)
                .videoCount((int) videoCount)
                .hasFullLength(hasFullLength)
                .hasPrimaryCover(hasPrimary)
                .meetsMinimumRequirements(meetsMinimumRequirements)
                .missingRequirements(missingRequirements)
                .build();
    }

    public void deleteMedia(User currentUser, Long mediaId) {
        UserMedia media = userMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (!media.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only delete your own media.");
        }
        userMediaRepository.delete(media);
    }

    public MediaUploadResponseDto updateMedia(User currentUser, Long mediaId, MediaUpdateRequestDto request) {
        UserMedia media = userMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (!media.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only update your own media.");
        }
        if (request.getCaption() != null) {
            media.setCaption(request.getCaption());
        }
        if (request.getDisplayOrder() != null) {
            media.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getMediaCategory() != null) {
            media.setMediaCategory(request.getMediaCategory());
        }
        if (request.getPrivacyMode() != null) {
            media.setPrivacyMode(request.getPrivacyMode());
        }
        if (request.getIsCover() != null) {
            media.setIsCover(request.getIsCover());
        }
        if (request.getPromptText() != null) {
            media.setPromptText(request.getPromptText());
        }
        if (request.getRequiresAccessApproval() != null) {
            media.setRequiresAccessApproval(request.getRequiresAccessApproval());
        }
        UserMedia updated = userMediaRepository.save(media);
        return mapToDto(updated);
    }

    public List<MediaUploadResponseDto> getAllMediaForAdmin() {
        return userMediaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MediaUploadResponseDto> getPendingMediaForAdmin() {
        return userMediaRepository.findByStatus(MediaStatus.PENDING).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MediaUploadResponseDto moderateMedia(Long mediaId, MediaModerationRequestDto request) {
        UserMedia media = userMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (request.getStatus() == MediaStatus.REJECTED &&
           (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new BadRequestException("Rejection reason is required when rejecting content.");
        }
        media.setStatus(request.getStatus());
        media.setRejectionReason(request.getStatus() == MediaStatus.REJECTED ? request.getRejectionReason() : null);
        UserMedia updated = userMediaRepository.save(media);
        return mapToDto(updated);
    }

    // 👈 Photo verification (Step 5): face comparison now runs 100% client-side
    // using face-api.js in the browser. The backend simply marks the user as
    // verified once the frontend confirms the match (POST /api/users/verify-success).
    // No image bytes are sent to any external service.

    private MediaUploadResponseDto mapToDto(UserMedia media) {
        return MediaUploadResponseDto.builder()
                .id(media.getId())
                .userId(media.getUser().getId())
                .userName(media.getUser().getFirstName() + " " + media.getUser().getLastName())
                // 👈 Dynamic streaming URL for frontend <img> and <video> tags
                .mediaUrl("/api/media/" + media.getId() + "/stream")
                .mediaType(media.getMediaType())
                .fileSizeKb(media.getFileSizeKb())
                .status(media.getStatus())
                .rejectionReason(media.getRejectionReason())
                .caption(media.getCaption())
                .displayOrder(media.getDisplayOrder())
                .mediaCategory(media.getMediaCategory())
                .privacyMode(media.getPrivacyMode())
                .isCover(media.getIsCover())
                .promptText(media.getPromptText())
                .requiresAccessApproval(media.getRequiresAccessApproval())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
