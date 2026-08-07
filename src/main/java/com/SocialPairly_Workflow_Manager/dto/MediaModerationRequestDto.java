package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import lombok.Data;

@Data
public class MediaModerationRequestDto {
    private MediaStatus status; // APPROVED or REJECTED
    private String rejectionReason;
}