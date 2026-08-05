package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaUploadResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private String mediaUrl;
    private MediaType mediaType;
    private Double fileSizeKb;
    private MediaStatus status;
    private String rejectionReason;
    private String caption;
    private Integer displayOrder;
    private String mediaCategory;
    private String privacyMode;
    private Boolean isCover;
    private String promptText;
    private Boolean requiresAccessApproval;
    private LocalDateTime createdAt;
}
