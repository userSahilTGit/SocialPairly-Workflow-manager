package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.StaffInboxNotification;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackRequest;
import com.SocialPairly_Workflow_Manager.entity.SupportCallbackStatus;

import java.time.LocalDateTime;

/**
 * Staff inbox row. {@code read} flips only when the related callback is marked contacted or closed;
 * the callback/handler fields show who acted on it.
 */
public record StaffInboxNotificationDto(
        Long id,
        String type,
        String title,
        String message,
        Long relatedCallbackId,
        String recipientRoleCode,
        boolean read,
        LocalDateTime createdAt,
        SupportCallbackStatus callbackStatus,
        Long handledByUserId,
        String handledByName,
        String handledByEmail,
        String handledByRole,
        LocalDateTime handledAt
) {
    public static StaffInboxNotificationDto from(StaffInboxNotification row) {
        return from(row, null);
    }

    public static StaffInboxNotificationDto from(StaffInboxNotification row, SupportCallbackRequest callback) {
        return new StaffInboxNotificationDto(
                row.getId(),
                row.getType(),
                row.getTitle(),
                row.getMessage(),
                row.getRelatedCallbackId(),
                row.getRecipientRoleCode(),
                row.isRead(),
                row.getCreatedAt(),
                callback != null ? callback.getStatus() : null,
                callback != null ? callback.getStatusUpdatedByUserId() : null,
                callback != null ? callback.getStatusUpdatedByName() : null,
                callback != null ? callback.getStatusUpdatedByEmail() : null,
                callback != null ? callback.getStatusUpdatedByRole() : null,
                callback != null ? callback.getStatusUpdatedAt() : null
        );
    }
}
