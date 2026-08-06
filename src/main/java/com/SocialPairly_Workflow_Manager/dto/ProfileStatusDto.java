package com.SocialPairly_Workflow_Manager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfileStatusDto {
    private boolean isProfilePublished;
    private boolean isVerified;
    private int photoCount;
    private int videoCount;
    private boolean hasFullLength;
    private boolean hasPrimaryCover;
    private boolean meetsMinimumRequirements;
    private List<String> missingRequirements;
}