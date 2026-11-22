package com.SocialPairly_Workflow_Manager.dto;

import com.SocialPairly_Workflow_Manager.entity.AppRating;

import java.time.LocalDateTime;

public record AppRatingDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Integer stars,
        String suggestion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppRatingDto from(AppRating row) {
        return new AppRatingDto(
                row.getId(),
                row.getUser() != null ? row.getUser().getId() : null,
                row.getUserName(),
                row.getUserEmail(),
                row.getStars(),
                row.getSuggestion(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
