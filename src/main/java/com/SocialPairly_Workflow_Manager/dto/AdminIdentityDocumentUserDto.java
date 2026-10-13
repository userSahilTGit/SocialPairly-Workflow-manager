package com.SocialPairly_Workflow_Manager.dto;

import java.util.List;

/** One member with their uploaded government ID images (ADO #1560). */
public record AdminIdentityDocumentUserDto(
        Long userId,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String idDocumentType,
        String idDocumentNumberMasked,
        List<AdminIdentityDocumentItemDto> documents
) {}
