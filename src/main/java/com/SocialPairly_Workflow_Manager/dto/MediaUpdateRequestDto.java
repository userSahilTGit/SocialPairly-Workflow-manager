package com.SocialPairly_Workflow_Manager.dto;

import lombok.Data;

@Data
public class MediaUpdateRequestDto {
    private String caption;
    private Integer displayOrder;
    private String mediaCategory;
    private String privacyMode;
    private Boolean isCover;
    private String promptText;
    private Boolean requiresAccessApproval;
}
