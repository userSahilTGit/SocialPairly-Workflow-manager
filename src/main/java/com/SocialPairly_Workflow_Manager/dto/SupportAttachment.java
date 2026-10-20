package com.SocialPairly_Workflow_Manager.dto;

/**
 * File attached to a customer-care message. Bytes are included on the support email.
 */
public record SupportAttachment(
        String filename,
        String contentType,
        byte[] data
) {
}
