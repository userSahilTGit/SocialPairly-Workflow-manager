package com.SocialPairly_Workflow_Manager.dto;

/**
 * Metadata for one identity ID image (no LONGBLOB bytes).
 * Admin UI loads pixels via {@code GET /api/onboarding/identity/documents/{id}/stream}.
 */
public record AdminIdentityDocumentItemDto(
        Long documentId,
        String docPurpose,
        String contentType,
        Integer fileSizeKb,
        String createdAt,
        String streamUrl
) {}
