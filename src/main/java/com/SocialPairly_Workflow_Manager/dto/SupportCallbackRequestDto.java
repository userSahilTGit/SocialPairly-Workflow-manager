package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;

import java.time.LocalDateTime;

public record SupportCallbackRequestDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String phoneNumber,
        String note,
        SupportCallbackStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        String resolvedByEmail,
        Long statusUpdatedByUserId,
        String statusUpdatedByName,
        String statusUpdatedByEmail,
        String statusUpdatedByRole,
        LocalDateTime statusUpdatedAt
) {
    public static SupportCallbackRequestDto from(SupportCallbackRequest row) {
        return new SupportCallbackRequestDto(
                row.getId(),
                row.getUser() != null ? row.getUser().getId() : null,
                row.getUserName(),
                row.getUserEmail(),
                row.getPhoneNumber(),
                row.getNote(),
                row.getStatus(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getResolvedAt(),
                row.getResolvedByEmail(),
                row.getStatusUpdatedByUserId(),
                row.getStatusUpdatedByName(),
                row.getStatusUpdatedByEmail(),
                row.getStatusUpdatedByRole(),
                row.getStatusUpdatedAt()
        );
    }
}
