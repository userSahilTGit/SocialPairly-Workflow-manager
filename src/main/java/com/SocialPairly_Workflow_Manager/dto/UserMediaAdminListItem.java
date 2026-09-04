package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.MediaStatus;
import com.SocialPairly_Workflow_Manager.entity.MediaType;

import java.time.LocalDateTime;

/**
 * Admin moderation list projection — intentionally excludes media_data LONGBLOB
 * so listing does not load every file into memory.
 */
public interface UserMediaAdminListItem {
    Long getId();
    Long getUserId();
    String getFirstName();
    String getLastName();
    String getPreferredName();
    String getEmail();
    MediaType getMediaType();
    Double getFileSizeKb();
    MediaStatus getStatus();
    String getRejectionReason();
    String getCaption();
    Integer getDisplayOrder();
    String getMediaCategory();
    String getPrivacyMode();
    Boolean getIsCover();
    String getPromptText();
    Boolean getRequiresAccessApproval();
    LocalDateTime getCreatedAt();
}
