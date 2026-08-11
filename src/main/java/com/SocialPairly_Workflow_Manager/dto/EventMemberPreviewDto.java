package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;

public record EventMemberPreviewDto(
        Long userId,
        String profilePhotoUrl,
        String initial
) {
}
